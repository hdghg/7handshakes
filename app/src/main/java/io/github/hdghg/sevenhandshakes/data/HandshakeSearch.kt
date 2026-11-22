package io.github.hdghg.sevenhandshakes.data

/** Stars [sourceContactId] gave to [targetContactId], summed over some of their reviews. */
data class ReviewEdge(
    val sourceContactId: Int,
    val targetContactId: Int,
    val stars: Int,
)

/**
 * Review graph between contacts. An edge A → B weighs all the stars A gave to B:
 * a review of 3 stars weighs as much as three reviews of 1 star.
 */
class HandshakeGraph(edges: List<ReviewEdge>) {
    // source -> target -> weight
    private val out: Map<Int, Map<Int, Int>> =
        edges
            .filter { it.stars > 0 && it.sourceContactId != it.targetContactId }
            .groupBy { it.sourceContactId }
            .mapValues { (_, out) -> out.groupingBy { it.targetContactId }.fold(0) { sum, edge -> sum + edge.stars } }

    /**
     * Ratings between [contactId] and [myContactId] over exactly [handshakes] edges.
     * Contacts in [ignored] are never used as intermediate steps; neither are the two ends.
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
        return ReviewExchange(
            receivedPath = widestPath(out, contactId, myContactId, handshakes, excluded, checkCancelled),
            givenPath = widestPath(out, myContactId, contactId, handshakes, excluded, checkCancelled),
        )
    }
}

/**
 * Best path from [from] to [to] using exactly [edges] edges, or null when there is no such path.
 *
 * A path weighs as much as its weakest edge; among several paths the heaviest one wins.
 * Paths never visit a vertex twice, and every vertex between the ends must be outside [excluded].
 *
 * [checkCancelled] is called every [CANCEL_CHECK_INTERVAL] search steps; it may throw to stop the search.
 */
internal fun widestPath(
    out: Map<Int, Map<Int, Int>>,
    from: Int,
    to: Int,
    edges: Int,
    excluded: Set<Int>,
    checkCancelled: () -> Unit = {},
): HandshakePath? {
    if (edges < 1 || from == to) return null

    fun canStep(target: Int, remainingAfter: Int): Boolean =
        if (remainingAfter == 0) target == to else target !in excluded

    // bound[r][v]: best weight from v to [to] in exactly r edges, ignoring the no-repeat rule.
    // Repeats can only add paths, so this is an upper bound used to cut the search short.
    val bound = ArrayList<Map<Int, Int>>(edges)
    bound.add(mapOf(to to Int.MAX_VALUE))
    for (r in 1 until edges) {
        val next = bound[r - 1]
        val level = HashMap<Int, Int>()
        for ((v, targets) in out) {
            if (v in excluded) continue
            var best = 0
            for ((w, weight) in targets) {
                if (!canStep(w, r - 1)) continue
                val through = minOf(weight, next[w] ?: 0)
                if (through > best) best = through
            }
            if (best > 0) level[v] = best
        }
        bound.add(level)
    }

    var best = 0
    var bestPath: HandshakePath? = null
    val visited = HashSet<Int>().apply { add(from) }
    val stack = arrayListOf(from)
    val stackWeights = ArrayList<Int>(edges)
    var steps = 0

    fun search(v: Int, remaining: Int, weight: Int) {
        if (++steps % CANCEL_CHECK_INTERVAL == 0) checkCancelled()
        val targets = out[v] ?: return
        val remainingAfter = remaining - 1
        // Heavier edges first, so a good answer is found early and prunes the rest.
        for ((w, edgeWeight) in targets.entries.sortedByDescending { it.value }) {
            if (!canStep(w, remainingAfter) || w in visited) continue
            val through = minOf(weight, edgeWeight)
            if (minOf(through, bound[remainingAfter][w] ?: 0) <= best) continue
            if (remainingAfter == 0) {
                best = through
                bestPath = HandshakePath(contactIds = stack + w, weights = stackWeights + edgeWeight)
            } else {
                visited.add(w)
                stack.add(w)
                stackWeights.add(edgeWeight)
                search(w, remainingAfter, through)
                stackWeights.removeAt(stackWeights.lastIndex)
                stack.removeAt(stack.lastIndex)
                visited.remove(w)
            }
        }
    }

    search(from, edges, Int.MAX_VALUE)
    return bestPath
}

/** How many search steps pass between two [widestPath] cancellation checks: often enough to stop quickly, rare enough to cost nothing. */
private const val CANCEL_CHECK_INTERVAL = 1024
