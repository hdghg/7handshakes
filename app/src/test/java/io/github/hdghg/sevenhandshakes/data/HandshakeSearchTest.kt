package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HandshakeSearchTest {
    private val me = 1
    private val contact = 2

    private fun edge(from: Int, to: Int, count: Int, level: Byte = 3) = ReviewEdge(from, to, level, count)

    private fun HandshakeGraph.at(handshakes: Int, ignored: Set<Int> = emptySet()) =
        exchange(contactId = contact, myContactId = me, handshakes = handshakes, ignored = ignored)

    @Test
    fun oneHandshakeIsDirectCounts() {
        val graph = HandshakeGraph(listOf(edge(contact, me, 4), edge(me, contact, 2, level = 1)))
        val result = graph.at(1)
        assertThat(result.receivedPaths.mapValues { it.value.weight }).containsExactly(3.toByte(), 4)
        assertThat(result.givenPaths.mapValues { it.value.weight }).containsExactly(1.toByte(), 2)
    }

    @Test
    fun pathWeighsAsItsWeakestEdge() {
        val graph = HandshakeGraph(listOf(edge(contact, 10, 5), edge(10, me, 2)))
        assertThat(graph.at(2).receivedOf(3)).isEqualTo(2)
        assertThat(graph.at(2).givenOf(3)).isEqualTo(0)
    }

    @Test
    fun heaviestPathWins() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 5), edge(10, me, 2),
                edge(contact, 11, 3), edge(11, me, 3),
            ),
        )
        assertThat(graph.at(2).receivedOf(3)).isEqualTo(3)
    }

    @Test
    fun returnsTheWinningPathWithEdgeWeights() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 5), edge(10, 11, 2), edge(11, me, 4),
                edge(contact, 12, 1), edge(12, 13, 9), edge(13, me, 9),
            ),
        )
        val path = graph.at(3).receivedPaths.getValue(3)
        assertThat(path.contactIds).containsExactly(contact, 10, 11, me).inOrder()
        assertThat(path.weights).containsExactly(5, 2, 4).inOrder()
        assertThat(path.weight).isEqualTo(2)
    }

    @Test
    fun shorterPathsDoNotCountForLongerLevels() {
        val graph = HandshakeGraph(listOf(edge(contact, me, 7), edge(contact, 10, 1), edge(10, me, 1)))
        assertThat(graph.at(2).receivedOf(3)).isEqualTo(1)
        assertThat(graph.at(3).receivedOf(3)).isEqualTo(0)
    }

    @Test
    fun endsAreNotUsedInTheMiddle() {
        // contact -> me -> 10 -> me would be 3 edges, but me cannot be passed through.
        val graph = HandshakeGraph(listOf(edge(contact, me, 5), edge(me, 10, 5), edge(10, me, 5)))
        assertThat(graph.at(3).receivedOf(3)).isEqualTo(0)
    }

    @Test
    fun verticesAreNotRevisited() {
        // contact -> 10 -> 11 -> 10 -> me is a walk, not a path.
        val graph = HandshakeGraph(
            listOf(edge(contact, 10, 5), edge(10, 11, 5), edge(11, 10, 5), edge(10, me, 5)),
        )
        assertThat(graph.at(4).receivedOf(3)).isEqualTo(0)
        assertThat(graph.at(2).receivedOf(3)).isEqualTo(5)
    }

    @Test
    fun ignoredContactsAreSkipped() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 5), edge(10, me, 5),
                edge(contact, 11, 1), edge(11, me, 1),
            ),
        )
        assertThat(graph.at(2, ignored = setOf(10)).receivedOf(3)).isEqualTo(1)
    }

    @Test
    fun ignoredEndsStillCount() {
        val graph = HandshakeGraph(listOf(edge(contact, me, 3)))
        assertThat(graph.at(1, ignored = setOf(contact)).receivedOf(3)).isEqualTo(3)
    }

    @Test
    fun levelsAreSearchedSeparately() {
        val graph = HandshakeGraph(
            listOf(edge(me, 10, 4, level = 2), edge(10, contact, 4, level = 1), edge(10, contact, 6, level = 2)),
        )
        val result = graph.at(2)
        assertThat(result.givenPaths.mapValues { it.value.weight }).containsExactly(2.toByte(), 4)
    }

    @Test
    fun givenAndReceivedUseTheirOwnPaths() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 3), edge(10, 11, 3), edge(11, me, 3),
                edge(me, 12, 2), edge(12, 13, 2), edge(13, contact, 2),
            ),
        )
        val result = graph.at(3)
        assertThat(result.receivedOf(3)).isEqualTo(3)
        assertThat(result.givenOf(3)).isEqualTo(2)
    }
}
