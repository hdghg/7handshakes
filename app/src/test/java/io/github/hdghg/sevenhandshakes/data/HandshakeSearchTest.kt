package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HandshakeSearchTest {
    private val me = 1
    private val contact = 2

    private fun edge(from: Int, to: Int, stars: Int) = ReviewEdge(from, to, stars)

    private fun HandshakeGraph.at(handshakes: Int, ignored: Set<Int> = emptySet()) =
        exchange(contactId = contact, myContactId = me, handshakes = handshakes, ignored = ignored)

    @Test
    fun oneHandshakeIsDirectStars() {
        val graph = HandshakeGraph(listOf(edge(contact, me, 4), edge(me, contact, 2)))
        val result = graph.at(1)
        assertThat(result.received).isEqualTo(4)
        assertThat(result.given).isEqualTo(2)
        assertThat(result.difference).isEqualTo(2)
    }

    @Test
    fun starsOfSeveralReviewsAddUp() {
        // Three reviews of 1 star and one of 3 stars from the same author.
        val graph = HandshakeGraph(listOf(edge(contact, me, 3), edge(contact, me, 3)))
        assertThat(graph.at(1).received).isEqualTo(6)
    }

    @Test
    fun pathWeighsAsItsWeakestEdge() {
        val graph = HandshakeGraph(listOf(edge(contact, 10, 5), edge(10, me, 2)))
        assertThat(graph.at(2).received).isEqualTo(2)
        assertThat(graph.at(2).given).isEqualTo(0)
        assertThat(graph.at(2).givenPath).isNull()
    }

    @Test
    fun heaviestPathWins() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 5), edge(10, me, 2),
                edge(contact, 11, 3), edge(11, me, 3),
            ),
        )
        assertThat(graph.at(2).received).isEqualTo(3)
    }

    @Test
    fun returnsTheWinningPathWithEdgeWeights() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 5), edge(10, 11, 2), edge(11, me, 4),
                edge(contact, 12, 1), edge(12, 13, 9), edge(13, me, 9),
            ),
        )
        val path = graph.at(3).receivedPath!!
        assertThat(path.contactIds).containsExactly(contact, 10, 11, me).inOrder()
        assertThat(path.weights).containsExactly(5, 2, 4).inOrder()
        assertThat(path.weight).isEqualTo(2)
    }

    @Test
    fun shorterPathsDoNotCountForLongerLevels() {
        val graph = HandshakeGraph(listOf(edge(contact, me, 7), edge(contact, 10, 1), edge(10, me, 1)))
        assertThat(graph.at(2).received).isEqualTo(1)
        assertThat(graph.at(3).received).isEqualTo(0)
    }

    @Test
    fun endsAreNotUsedInTheMiddle() {
        // contact -> me -> 10 -> me would be 3 edges, but me cannot be passed through.
        val graph = HandshakeGraph(listOf(edge(contact, me, 5), edge(me, 10, 5), edge(10, me, 5)))
        assertThat(graph.at(3).received).isEqualTo(0)
    }

    @Test
    fun verticesAreNotRevisited() {
        // contact -> 10 -> 11 -> 10 -> me is a walk, not a path.
        val graph = HandshakeGraph(
            listOf(edge(contact, 10, 5), edge(10, 11, 5), edge(11, 10, 5), edge(10, me, 5)),
        )
        assertThat(graph.at(4).received).isEqualTo(0)
        assertThat(graph.at(2).received).isEqualTo(5)
    }

    @Test
    fun ignoredContactsAreSkipped() {
        val graph = HandshakeGraph(
            listOf(
                edge(contact, 10, 5), edge(10, me, 5),
                edge(contact, 11, 1), edge(11, me, 1),
            ),
        )
        assertThat(graph.at(2, ignored = setOf(10)).received).isEqualTo(1)
    }

    @Test
    fun ignoredEndsStillCount() {
        val graph = HandshakeGraph(listOf(edge(contact, me, 3)))
        assertThat(graph.at(1, ignored = setOf(contact)).received).isEqualTo(3)
    }

    @Test
    fun selfReviewsAndEmptyEdgesAreDropped() {
        val graph = HandshakeGraph(listOf(edge(contact, contact, 9), edge(contact, me, 0)))
        assertThat(graph.at(1).receivedPath).isNull()
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
        assertThat(result.received).isEqualTo(3)
        assertThat(result.given).isEqualTo(2)
    }
}
