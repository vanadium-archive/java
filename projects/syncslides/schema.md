# Overview

 Syncslides uses Syncbase for data storage and communication between users' devices.

 # Detailed Design
 ```
 // Deck is the metadata for a set of slides.
 struct Deck {
   Title string
   Thumbnail []byte
 }

 // Slide contains the image content for the slide.
 struct Slide {
   // A low-res version of the image stored as PNG.
   Thumbnail []byte
   // A high-res version of the image stored as PNG.
   Fullsize Blobref
 }

 // Note contains private-to-the user notes for a specific slide.
 struct Note {
   Text string
 }

 // Presentation represents a live display of a Deck.
 struct Presentation {
   // If the presenter has handed control of the presentation to an audience member,
   // driver will be present.  When the presenter takes back control, this
   // will switch back to the empty string.
   Driver ?Person  // ? means optional.
 }

 struct Person {
   FirstName string
   LastName string
 }

 // CurrentSlide contains state for the live presentation.  It is separate from the
 // Presentation so that the presenter can temporarily delegate control of the
 // CurrentSlide without giving up control of the entire presentation.
 struct CurrentSlide {
   // The number of the slide that the presenter is talking about.
   Num int32
   // In the future, we could add markup/doodles here.  That markup would be transient
   // if stored here.  Maybe better to put it in a separate row...
 }

 // Question represents a member of the audience asking a question of the presenter.
 // TODO(kash): Add support for the user to type in their question.  Right now, they
 // need to ask their question verbally.
 struct Question {
   // The person who asked the question.
   Questioner Person
   // Track whether this question has been answered.
   Answered bool
 }
 ```

 ## Table `Decks`

 A Deck is a set of slides plus metadata.  It can be used in multiple presentations.  It is owned
 by the presenter, and the audience has read-only access.

 The deck is immutable because we use the simple scheme of ordering slides by their
 key (see example below).  The other tables refer to the slides by these hardcoded
 names, so those references would break if we allowed deck mutations.
 ```
 <deckId>          --> Deck
 <deckId>/slide1   --> Slide
 <deckId>/slide2   --> Slide
 <deckId>/slide3   --> Slide
 ...
 ```

 ## Table `Notes`

 Notes are private to a user.  They are sparse in that if a user does not have any
 notes for a slide, the corresponding row is not present.

 The `lastViewed` row contains the timestamp that the user last viewed the presentation
 in milliseconds since the epoch.
 TODO(kash): Can we replace this with vdl.Time?  Does it work in Java?
 ```
 <deckId>/LastViewed  --> int64
 <deckId>/slide1      --> Note
 <deckId>/slide5      --> Note
 ```

 ## Table `Presentations`

 A presentation represents the live state of a presenter displaying a deck to an audience.
 ```
 <deckId>/<presentationId>                         --> Presentation
 <deckId>/<presentationId>/CurrentSlide            --> CurrentSlide
 <deckId>/<presentationId>/questions/<questionId>  --> Question
 ```

 ## Syncgroups

 There are multiple syncgroups as part of a live presentation.
 * The presentation syncgroup contains:
   * Table: Decks, Prefix: `<deckId>`
     * ACL: Presenter: RWA, Audience: R
   * Table: Presentation, Prefix: `<deckId>/<presentationId>`
     * ACL: Presenter: RWA, Audience: R
 * The notes syncgroup contains:
   * Table: Notes, Prefix: `<deckId>`
     * ACL: Person who wrote the notes: RWA

 ## Delegation

 We want to be able to temporarily delegate control of the presentation to an audience member.
 The presenter will do this by writing the audience member's name to the Presentation struct
 in the Presentations table.  The presenter will also set the ACL on the CurrentSlide so that
 audience member can write it.  When the presenter wants control again, she will reverse these
 steps.

 ## Questions

 When a user wants to ask a question, their device needs to write a Question struct into the
 Presentations table.  The `<deckId>/<presentationId>/questions` prefix will be writable
 to everyone.  When the questioner adds a question, they will also add an ACL just for that
 row.  The ACL will give write access to that user and to the presenter.