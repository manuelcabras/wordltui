## Game display
```
_____________________
|   | S |   |   | E |
=====================
eliminated letters : ADFX
guessed, wrong position : BCP
remaining attempts : 3

Answer with :
GUESS [word]
HINT [V/C]
```

## Commands

> general commands

S : WELCOME - HELP FOR COMMANDS

C : PLAY : start a game

C: QUIT
S: BYE

C: HELP
S: 
PLAY 
QUIT
HINT []
GUESS []
etc

C : HIGHSCORE [word]
S : if word -> highscore; else "no highscore for this word"

> game commands

C : HINT : ask a hint once
    S  : vowel or consonant
    C: V / C
    S: [letter]

C : GUESS [word]
S : main interface

C: FF // GIVEUP
S: YOU LOST THE GAME. WORD WAS [XXXX]

##  CODES :
200 : ok

## Error types :
- Game commands outside a game
- Guess invalid input (not a word, outside of dictionary etc.) 
- ask for hint twice
- invalid command / parameter
