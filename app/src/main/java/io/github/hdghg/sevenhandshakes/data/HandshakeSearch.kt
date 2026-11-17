package io.github.hdghg.sevenhandshakes.data

/** Number of reviews [sourceContactId] wrote to [targetContactId] with one rate level. */
data class ReviewEdge(
    val sourceContactId: Int,
    val targetContactId: Int,
    val rateLevel: Byte,
    val count: Int,
)

/**
 * Review graph between contacts, split by rate level.
 * An edge A → B weighs the number of reviews A wrote to B at that level.
 */
class HandshakeGraph(edges: List<ReviewEdge>) {
    // rate level -> source -> target -> weight
    private val byLevel: Map<Byte, Map<Int, Map<Int, Int>>> =
        edges
            .filter { it.count > 0 && it.sourceContactId != it.targetContactId }
            .groupBy { it.rateLevel }
            .mapValues { (_, levelEdges) ->
                levelEdges
                    .groupBy { it.sourceContactId }
                    .mapValues { (_, out) ->
                        out.groupingBy { it.targetContactId }.fold(0) { sum, edge -> sum + edge.count }
                    }
            }

    // rate level -> source -> target -> weight at that level and every level above it
    private val cumulative: Map<Byte, Map<Int, Map<Int, Int>>> = buildMap {
        var sum = emptyMap<Int, Map<Int, Int>>()
        for (level in byLevel.keys.sortedDescending()) {
            sum = sum + byLevel.getValue(level)
            put(level, sum)
        }
    }

    private operator fun Map<Int, Map<Int, Int>>.plus(other: Map<Int, Map<Int, Int>>): Map<Int, Map<Int, Int>> {
        val result = HashMap<Int, Map<Int, Int>>(this)
        for ((source, targets) in other) {
            val merged = HashMap(result[source] ?: emptyMap())
            for ((target, weight) in targets) merged[target] = (merged[target] ?: 0) + weight
            result[source] = merged
        }
        return result
    }

    /**
     * Ratings between [contactId] and [myContactId] over exactly [handshakes] edges.
     * Contacts in [ignored] are never used as intermediate steps; neither are the two ends.
     *
     * A better review also counts for the levels below it: an edge weighs its reviews of the level
     * plus those of every higher level. One edge of each path is the exception and weighs its
     * reviews of the level alone, so a path of only better reviews does not count for it.
     *
     * [checkCancelled] is called now and then during the search; it may throw to stop it early.
     */
    fun exchange(
        contactId: Int,
        myContactId: Int,
        handshakes: Int,
        ignored: Set<Int>,
        checkCancelled: () -> Unit = {},
    ): ReviewExchange {
        val excluded = ignored + contactId + myContactId
        val received = mutableMapOf<Byte, HandshakePath>()
        val given = mutableMapOf<Byte, HandshakePath>()
        for ((level, own) in byLevel) {
            val out = cumulative.getValue(level)
            widestPath(out, contactId, myContactId, handshakes, excluded, own, checkCancelled)
                ?.let { received[level] = it.withLevels(level) }
            widestPath(out, myContactId, contactId, handshakes, excluded, own, checkCancelled)
                ?.let { given[level] = it.withLevels(level) }
        }
        return ReviewExchange(receivedPaths = received, givenPaths = given)
    }

    /**
     * Adds the best rate level counted in each edge of a path found for [level]: [level] itself
     * for an edge weighing its own reviews only, else the best level with a review on that edge.
     */
    private fun HandshakePath.withLevels(level: Byte): HandshakePath {
        fun count(of: Byte, edge: Int): Int = byLevel[of]?.get(contactIds[edge])?.get(contactIds[edge + 1]) ?: 0
        return copy(
            levels = weights.mapIndexed { edge, weight ->
                if (weight == count(level, edge)) {
                    level
                } else {
                    byLevel.keys.filter { it > level && count(it, edge) > 0 }.maxOrNull() ?: level
                }
            },
        )
    }
}

/**
 * Best path from [from] to [to] using exactly [edges] edges, or null when there is no such path.
 *
 * A path weighs as much as its weakest edge; among several paths the heaviest one wins.
 * Paths never visit a vertex twice, and every vertex between the ends must be outside [excluded].
 *
 * Edges weigh as in [out], except for one edge of the path that weighs as in [pure] instead and
 * so has to exist there. The search picks that edge so that the path comes out heaviest; in the
 * returned path it carries its [pure] weight. By default [pure] is [out] and changes nothing.
 *
 * [checkCancelled] is called every [CANCEL_CHECK_INTERVAL] search steps; it may throw to stop the search.
 */
internal fun widestPath(
    out: Map<Int, Map<Int, Int>>,
    from: Int,
    to: Int,
    edges: Int,
    excluded: Set<Int>,
    pure: Map<Int, Map<Int, Int>> = out,
    checkCancelled: () -> Unit = {},
): HandshakePath? {
    if (edges < 1 || from == to) return null

    fun canStep(target: Int, remainingAfter: Int): Boolean =
        if (remainingAfter == 0) target == to else target !in excluded

    fun pureWeight(v: Int, w: Int): Int = pure[v]?.get(w) ?: 0

    // bound[r][v]: best weight from v to [to] in exactly r edges, ignoring the no-repeat rule.
    // Repeats can only add paths, so this is an upper bound used to cut the search short.
    // boundPure is the same for paths that still have to pass their pure edge.
    val bound = ArrayList<Map<Int, Int>>(edges)
    val boundPure = ArrayList<Map<Int, Int>>(edges)
    bound.add(mapOf(to to Int.MAX_VALUE))
    boundPure.add(emptyMap())
    for (r in 1 until edges) {
        val next = bound[r - 1]
        val nextPure = boundPure[r - 1]
        val level = HashMap<Int, Int>()
        val levelPure = HashMap<Int, Int>()
        for ((v, targets) in out) {
            if (v in excluded) continue
            var best = 0
            var bestPure = 0
            for ((w, weight) in targets) {
                if (!canStep(w, r - 1)) continue
                val rest = next[w] ?: 0
                val through = minOf(weight, rest)
                if (through > best) best = through
                // Either this edge is the pure one, or the pure one comes later.
                val throughPure = maxOf(minOf(pureWeight(v, w), rest), minOf(weight, nextPure[w] ?: 0))
                if (throughPure > bestPure) bestPure = throughPure
            }
            if (best > 0) level[v] = best
            if (bestPure > 0) levelPure[v] = bestPure
        }
        bound.add(level)
        boundPure.add(levelPure)
    }

    var best = 0
    var bestPath: HandshakePath? = null
    val visited = HashSet<Int>().apply { add(from) }
    val stack = arrayListOf(from)
    val stackWeights = ArrayList<Int>(edges)
    var steps = 0

    fun search(v: Int, remaining: Int, weight: Int, pureDone: Boolean) {
        if (++steps % CANCEL_CHECK_INTERVAL == 0) checkCancelled()
        val targets = out[v] ?: return
        val remainingAfter = remaining - 1

        fun step(w: Int, edgeWeight: Int, pureDoneAfter: Boolean) {
            val through = minOf(weight, edgeWeight)
            // With no edges left a path without its pure edge has bound 0, so it is dropped here too.
            val rest = (if (pureDoneAfter) bound else boundPure)[remainingAfter][w] ?: 0
            if (minOf(through, rest) <= best) return
            if (remainingAfter == 0) {
                best = through
                bestPath = HandshakePath(contactIds = stack + w, weights = stackWeights + edgeWeight)
            } else {
                visited.add(w)
                stack.add(w)
                stackWeights.add(edgeWeight)
                search(w, remainingAfter, through, pureDoneAfter)
                stackWeights.removeAt(stackWeights.lastIndex)
                stack.removeAt(stack.lastIndex)
                visited.remove(w)
            }
        }

        // Heavier edges first, so a good answer is found early and prunes the rest.
        for ((w, edgeWeight) in targets.entries.sortedByDescending { it.value }) {
            if (!canStep(w, remainingAfter) || w in visited) continue
            if (pureDone) {
                step(w, edgeWeight, pureDoneAfter = true)
            } else {
                step(w, pureWeight(v, w), pureDoneAfter = true)
                step(w, edgeWeight, pureDoneAfter = false)
            }
        }
    }

    search(from, edges, Int.MAX_VALUE, pureDone = false)
    return bestPath
}

/** How many search steps pass between two [widestPath] cancellation checks: often enough to stop quickly, rare enough to cost nothing. */
private const val CANCEL_CHECK_INTERVAL = 1024
