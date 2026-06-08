# Monopoly Deal Card Game

This repository contains a JavaFX implementation of **Monopoly Deal** for the
Software Engineering Project. The project is not only a card game
implementation; it also demonstrates requirements modelling, object-oriented
design, GUI development, networking, testing, and documentation.

Players can play locally on one device or connect through the online lobby.
During a match, players draw cards, play money/property/action cards, collect
rent, respond with Just Say No, pay rent or debt using valid assets, and try to
win by completing three full property sets.

## Project Goals

The course brief asks the team to implement Monopoly Deal in Java while using
software engineering best practices. This repository addresses the main Phase 2
assessment areas as follows:

| Assessment area | Project evidence |
| --- | --- |
| Clean design | The code is separated into model, engine, controller, gameplay service, network, protocol, and UI packages. |
| Interfaces | `PayableAsset` defines a shared payment-value contract for cards that can be used as money or payment. |
| Functionality | The project implements the main Monopoly Deal turn flow, property rules, action cards, rent, payment, win checking, local play, and online play. |
| GUI | JavaFX and FXML are used for the lobby, game board, card display, settings, achievements, and victory screen. |
| Team repository | The project is maintained in GitHub and uses a standard Maven project structure. |
| Testing | The test suite covers game engine rules, card behaviour, payment logic, DTO mapping, and online session flow. |
| Documentation | Supporting documents and revised UML materials are stored in the `docs` folder. |

## Quick Start

### Requirements

- Java 21
- Maven wrapper included in this repository
- JavaFX dependencies are downloaded by Maven

### Run the Application

On Windows:

```powershell
.\mvnw.cmd javafx:run
```

On macOS or Linux:

```bash
./mvnw javafx:run
```

### Run Tests

On Windows:

```powershell
.\mvnw.cmd test
```

On macOS or Linux:

```bash
./mvnw test
```

Latest local verification:

```text
Tests run: 93
Failures: 0
Errors: 0
Skipped: 0
```

## Application Modes

### Local Hot-Seat Mode

Local mode starts a game on one device. Players take turns on the same screen.
The local controller owns the `GameEngine` instance directly and updates the
JavaFX game board after each action.

### Online Multiplayer Mode

Online mode uses a host/join lobby:

1. The host enters a name and starts a server.
2. Other players enter the host address and port, then join the lobby.
3. The host can start once the lobby has at least 2 players.
4. The online session supports 2 to 4 players.
5. During the match, clients send commands such as draw, play card, discard,
   respond, and end turn.
6. The server validates the command, updates the game state, and broadcasts a
   player-specific view back to each client.

The lobby defaults to `127.0.0.1` and port `5947`, which is useful for local
testing.

## Implemented Game Features

- Local hot-seat gameplay.
- Online lobby for hosting and joining multiplayer games.
- 2 to 4 player online sessions.
- Initial dealing of five cards to each player.
- Drawing two cards at the start of a turn.
- Maximum of three card plays per turn.
- End-of-turn hand limit enforcement: players must discard down to seven cards.
- Money cards, property cards, wild property cards, rent cards, and action
  cards.
- Banked card values for money, action cards, and bankable wild property cards.
- Property placement by color.
- Wild property color selection.
- Complete set detection by property color.
- Protection for complete property sets in normal payment and stealing rules.
- Rent calculation based on owned billable properties, set size, and
  house/hotel improvements.
- Rent and debt payment using valid bank cards or valid properties.
- Just Say No response handling for targeted actions.
- Win detection when a player completes three full property sets.
- Achievement progress saved locally.
- Victory screen and online rematch flow.

## Game Logic

### Match Setup

- A new match creates players and a shuffled Monopoly Deal deck.
- Each player receives five cards.
- The current player starts with zero plays used and has not drawn yet.
- The discard pile is available for used cards and discarded cards.
- If the draw deck becomes empty, the discard pile can be reshuffled back into
  the deck.

### Turn Flow

Each turn follows this structure:

1. The current player draws two cards.
2. The player may play up to three cards.
3. A played card can be deposited as money, placed as property, used as an
   action, or used as a rent card depending on its type.
4. If the player uses all three plays, the turn attempts to end automatically.
5. If the player has more than seven cards, they must discard before the turn
   can fully end.
6. The game advances to the next player unless a win condition has been met.

The hand limit is enforced at the end of the turn rather than immediately after
drawing. This matches the normal Monopoly Deal turn structure.

### Card Play Rules

| Card type | Implemented behaviour |
| --- | --- |
| Money card | Goes into the player's bank and provides payment value. |
| Property card | Goes to the player's property area under its color. |
| Wild property card | Can be placed after selecting an available color; some wild cards can also be banked. |
| Rent card | Charges rent based on a valid owned property color. |
| Action card | Can be used for its effect or deposited into the bank for its bank value. |

### Property and Rent Rules

- Each color has a required set size.
- Brown, dark blue, and light green sets require 2 billable properties.
- Black requires 4 billable properties.
- Most other colors require 3 billable properties.
- Complete sets are protected from normal rent/debt payment and some stealing
  effects.
- House and Hotel cards improve complete sets and increase rent.
- Set improvements increase rent but do not count as normal billable properties.
- Rent cards only charge colors that the player can legally charge.
- Double the Rent applies to the next rent card and then resets.

### Payment Rules

The payment system is handled through `PayableAsset` and payment helper classes.
Cards that can provide monetary value implement this interface:

```java
public interface PayableAsset {
    int getPaymentValueM();
}
```

Payment behaviour:

- Rent and debt are paid with whole cards.
- Bank cards are valid payment assets.
- Properties outside complete sets can be used as payment when needed.
- Properties in complete sets are protected from normal payment.
- If the payer cannot pay the exact amount, the transferred value may exceed the
  required amount because cards cannot be split.
- In local play, the payer chooses which valid asset to transfer.
- In online play, the server tracks pending payment and response prompts.

### Action Card Rules

| Action card | Conditions and effect |
| --- | --- |
| Pass Go | Draws two additional cards. |
| Debt Collector | Chooses one opponent and charges 5M. The target may respond with Just Say No. |
| My Birthday | Charges each other player 2M. Opponents may respond where applicable. |
| Sly Deal | Steals one valid property from another player, excluding protected complete sets. |
| Forced Deal | Swaps one property with another player's valid property. |
| Deal Breaker | Steals a complete property set from another player. |
| Double the Rent | Must be paired with a rent card and uses two plays; doubles that rent charge. |
| House | Adds a house bonus to one of the player's complete property sets. |
| Hotel | Adds a hotel bonus to one of the player's complete property sets. |
| Just Say No | Used as a response to block a targeted action played against the defender. |

### Win Condition

A player wins when they complete three full property sets. The engine checks the
current player's property colors after successful card plays and action
resolutions.

## Code Organization

The project is organized by responsibility rather than by screen only. The main
modules are:

| Module | Responsibility | Important classes |
| --- | --- | --- |
| Model | Stores the core game objects and card hierarchy. | `Card`, `MoneyCard`, `PropertyCard`, `WildpropertyCard`, `RentCard`, `ActionCard`, `Player`, `AchievementManager` |
| Engine | Contains the core Monopoly Deal rules that should not depend on JavaFX. | `GameEngine`, `Deck`, `DeckFactory`, `DiscardPile`, `RentTable`, `PropertyRules`, `PaymentTransfer`, `RentPayment` |
| Gameplay services | Keeps complex gameplay workflows out of the UI controller. | `ActionEffectResolver`, `PaymentService`, `JustSayNoService`, `HandDiscardDialogService` |
| Controllers | Connects user actions to the game engine and updates screens. | `LobbyController`, `GameController`, `NetworkGameController` |
| Network | Supports online lobby, client/server messages, and player-specific game state. | `GameServer`, `GameSession`, `ClientHandler`, `NetworkClient`, `ServerPlayHandler` |
| Protocol and mapping | Converts game objects to data objects that can be sent over the network. | `ClientMessage`, `ServerMessage`, `GameStateDto`, `PlayerViewDto`, `CardMapper`, `GameStateMapper` |
| UI | Handles JavaFX application setup, card display, settings, scene navigation, achievements, and victory effects. | `MonopolyDealApp`, `SettingsOverlay`, `CardView`, `GameVictoryScreen`, `GameAlertDialogs` |
| Resources | Stores FXML screens, CSS, card images, avatar image, and background music. | `game-view.fxml`, `network-game-view.fxml`, `lobby-view.fxml`, `game-theme.css` |
| Tests | Verifies the important behaviours of the game. | Engine tests, card/action-card tests, payment tests, network tests, image loading tests |

This structure helps separate the game rules from the interface. For example,
`GameEngine` controls turns and win checking, while `GameController` and
`NetworkGameController` focus on user interaction and screen updates.

## Architecture and Design

### MVC-Style Separation

The project separates:

- **Model:** cards, players, enums, achievements.
- **Engine:** turn rules, draw rules, rent, payment, property set rules, win
  checking.
- **Controller:** local and online user interactions.
- **View/UI:** JavaFX/FXML views, custom card rendering, board layout, dialogs,
  settings, and animations.

This keeps most core game rules outside the JavaFX view layer.

### Interface Usage

`PayableAsset` is a custom interface for card types that can provide money value
when they are banked or used as payment. It is implemented by:

- `MoneyCard`
- `PropertyCard`
- `WildpropertyCard`
- `ActionCard`

This avoids duplicating payment-value logic across the player, rent, and payment
transfer code.

### Design Patterns and Techniques

| Pattern or technique | Where it appears |
| --- | --- |
| **MVC** | `model` and `engine` hold data/rules, `controller` handles user actions, and `ui` / `ui.render` handle visual rendering. |
| **Factory** | `DeckFactory` builds the game deck; `RentCard.allColors()` and `RentCard.dual()` create rent card variants. |
| **Facade/service layer** | `LocalGameSession`, `LocalCardPlayService`, `OnlineCardPlayService`, `PaymentService`, and `ActionEffectResolver` wrap multi-step gameplay workflows behind clearer service methods. |
| **Observer-style callback** | `NetworkClient` receives server messages and notifies `NetworkGameController` through a listener callback. |
| **DTO and Mapper** | `GameStateDto`, `CardDto`, `CardMapper`, and `GameStateMapper` separate network data transfer from domain objects. |
| **Singleton-style utility/manager** | `AchievementManager`, `GameSettings`, `JsonUtil`, and `CardImageLoader` centralize shared state or utility behaviour. |
| **Interface contract** | `PayableAsset` gives money, property, wild property, and action cards a shared payment-value contract. |
| **Polymorphism** | Concrete action cards provide their own behaviour through the shared `ActionCard` hierarchy. |

## Testing Strategy

The test suite focuses on meaningful game behaviour rather than simple getters
and setters.

| Test area | Examples |
| --- | --- |
| Engine rules | Drawing, turn state, hand limit, win checking, deck/discard behaviour |
| Payment logic | Rent collection, bank-first payment, property payment, complete set protection |
| Card model | Money cards, property cards, wild property cards, rent calculation |
| Action cards | Pass Go, Debt Collector, My Birthday, Sly Deal, Forced Deal, Deal Breaker, Double the Rent, House, Hotel, Just Say No |
| Networking | Card mapping, JSON round trip, server-side game session flow |
| UI support | Card image loading |

Current result:

```text
Tests run: 93
Failures: 0
Errors: 0
Skipped: 0
```

## Documentation

- [Phase 1 feedback response](docs/phase1_feedback_response.md)
- [Revised UML use case diagram](docs/use_case_diagram_revised_final.png)

The feedback response document explains how the team responded to Phase 1
comments, including the revised use case diagram and the reasoning behind the
updated UML structure.

## Demo Route

A recommended interview/demo flow:

1. Open the JavaFX application.
2. Show the lobby and enter a player name.
3. Demonstrate local mode or online host/join mode.
4. Start a match and show the initial five-card hand.
5. Draw two cards.
6. Play a money card into the bank.
7. Place a property card and show the public property board.
8. Use an action card such as Pass Go, Debt Collector, or Sly Deal.
9. Play a rent card and show the payment selection.
10. Demonstrate Just Say No if a target player has the card.
11. End the turn and show discard enforcement if hand size is above seven.
12. Show win detection after three complete property sets.

## Future Improvements

- Add more automated GUI tests for complete player workflows.
- Package the JavaFX application as an installable desktop build.
- Add more in-game rule hints for first-time players.
- Extend online deployment support beyond controlled local/demo sessions.
