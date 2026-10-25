# Seven Handshakes

Seven Handshakes is an app aimed to encourage people help each other.

I believe that we would be more helpful if we were able to see how our actions
return to us through other people. Seven handshakes aims to make this apparent
by keeping record of our positive actions, and sharing these records with the people
we interact with. For example, imagine yourself helping a stranger with food,
then that stranger helps some another stranger with a place to sleep, and then
that another stranger helps you with clothes. Normally it could happen that nobody
of the three actors think that their actions were related. But seven handshakes'
goal is to make this connection visible.

# Which problems does this app aim to solve

## 1. Encourage volunteering

Many companies try to involve their employees and clients to participate in
volunteering and charity. Each of the companies invents their own rating system from
scratch, repeating each other. If there were community-driven OpenSource initiative
that does all the common job, they could have spared energy and money

## 2. Independent rating system

We have already accustomed to rate our parthers. We rate couriers, taxi drivers,
support managers. Some companies encourage their employees to perform annual
reviews of their peers. These review live in domain of the companies that
implemented them, and people lose access to reviews thay received as soon as
they leave. So Seven Handshakes aims to build a system that allows people to keep
their reviews from other people on their own smartphones or any other device.


## 3. Integrating AI agents as conscious beings in our world

As of autumn 2026, artificial intelligence models are trained to achieve  best
results. What exactly means "best" is decided by only a few people. I believe
that our history has clearly demonstrated us what happens when we give power to
people that are the "best". My vision is that AI agents and autonomous robots
need a way to receive feedback of their actions. They must not be trained based
on what is right and what is wrong by laws of corporate world. Instead, these
entities must learn based on feedback they receive everyday from people they
interact with. By using common rating system between robots and humans we have
great chances that robots will be able to learn what people need and integrate
in society naturally.

## 4. Gen Z generation poverty

As of autumn 2026, I often hear disturbing conclusions that Gen Z was robbed
of their future. They are predicted to end up without home, family and property.
Many countries experience decline in population already, and with zoomers entering
reproductive age the situation could become checkmate to us all. As a solution
I want to suggest everyone to turn away from rat race for money, instead try
to help our peers. Seven Handshakes is intended to help with this by encouraging
to help each other. We are more eager to help when we see a person that is helping
others, and even have helped us indirectly. In the end we shouldn't see each
other as a stranger, but as a neighbor

# Principles and compass

## Offline first

I see seven handshakes as a distributed system that works offline or with limi ted
connectivity.
1. I can't donate for servers to keep ever-growing data, and I don't want to raise
funds
2. I want to make people feel that they keep their data, and it will not be lost
during crisis or some commercial company's bankruptcy.
3. It's easier to match data regulations that many countries have

## Not for negative review

I don't see how negative reviews fit into seven handshakes' goals. I see that
the app gives opportunity to rate receiver with 1 to 3 stars:
- 1 star: small gratitude
- 2 stars: normal gratitude
- 3 stars: great gratitude

For those who want to place more than 3 stars, I suggest making one more review.
I also see that review can be recalled. Recall is a separate action, equivalent
to the review action.

## About sharing
I want to implement many ways of sharing reviews, not only with those who receive
the review, but also with whoever.
1. Share via QR code
2. Share as a deep link sent to any messenger
3. Bulk share vie email in an archive

Bulk sharing is seen by me as a significant part of seven handshakes.
1. People may want to share daily with their relatives and close friends, so they
know who I contacted with. So in future tye can pay back for me (or be paid back).
2. Share with those employers who encourage volunteering, as described above
3. Making regular backups
4. Sharing with those strangers that help us, or receive help from us, so we can
immediately trace whether we were connected by our actions through common actors

I intend to add features that schedule export and import by email. The import is
seen to be filtered. I see that I should only import those records that relate to me
directly, or indirectly within limited depths of "handshakes".

## Abouts signature
In order to discourage abuse I plan to implement each review be signed.
Private key of singer is meant to be stored internally on their device, and public
key being shared along with the signature, review and gratitude rating (1 to 3 stars)

I chose ed25519 as a signature with most brief public key.
I see that each review must be verified during import.

## About abuse
I see that some parties may want to abuse seven handshakes in a few ways. Here
are some of the examples and my countermeasures.
### 1. Abuser creates fake private keys and creates fake positive reviews for himself.
I intend to filter out fake reviews during import. If those reviewers are not
know to me within N handshakes to recipient then during import those reviews
are discarded.
### 2. Abuser is within my N handshakes, but I still don't trust him
It should be allowed to distrust any public key. The key is to be filtered out
during import.

## About desktop versions and iPhone
Seven handshakes is seen as cross-platform system, so these architectures are welcome.
I focus on android now because it's easier to me. I hope to leverage compose
multiplatform at some point to quickly port the app to iPhone and desktop.

## About servers
Application is designed to be serverless, but some additional functions surely can
live in cloud. One example is backups. Another example could be server for those
organizations who encourage volunteering: Server receives reviews from volunteering
organizations, matches them with their clients/employees and automatically awards
points to those people

## About contributing
I welcome contributions. I shared my vision of product's evolution, but some decisions
and principles are surely to be maid in the future. In case of conflict I commit to
stick to those decisions that are more adherent to
[The Noble Eightfold Path](https://en.wikipedia.org/wiki/Noble_Eightfold_Path)

## About AI-assisted development
I welcome AI assisted development. In my opinion, AI agent must be treated as a partner
in pair programming software development technique. That means that contributors are
in responsibility to deliver decent quality code.
