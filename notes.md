There are two categories of keys: chording keys, and everything else.

Scenarios:

Assume:

* j = right shift
* k = right ctrl
* l = right alt
* ; = right 'special'
* f = left shift
* d = left ctrl
* s = left alt
* a = left 'special'

# Chording key regular key press

|j down|Nothing|
|j up  |j down, j up|

# Chording key held down

Not sure what should happen here. Seems like it would be nice to get
repeats. But the key repeat delay is pretty quick. Does it adjust if
you change the keyboard settings?

Also, what if you're chording several modified keys? Would it start to
repeat on you? I think we might have to give up repeats.

Actually, thinking more about it, it looks like we could intercept the
first few repeat events and throw them away, until a suitable interval
has passed and we can interpret the key down as an intent to repeat,
and not to modify. For the first release, though, I think it makes
more sense to just ignore repeats for chording keys.

|j down|Nothing|
|j down|j down?|
|j down|j down?|
|j down|j down?|
|j down|j down?|
|j down|j down?|
|j down|j down?|
|j up  |j up   |
