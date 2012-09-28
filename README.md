# khordr

A pluggable framework for intercepting the key events that occur at a
hardware level and doing "things" in response. "Things" can include
running programs, altering the keystrokes to other keystrokes or
combinations of keystrokes, or anything you can write code to do.
There will be built-in support for doing things like moving the mouse
from the keys on your keyboard.

The name "khordr" comes from the fact that the primary use case that
motivated me to write this is to use key "chords" to accomplish
things. A chord, like on a piano keyboard, is when more than one key
is pressed simultaneously.

# Genesis

I've always been interested in typing. As a computer programmer, it's
the primary mechanism I use to do my job. I've been very lucky not to
experience any problems with
[RSI](http://en.wikipedia.org/wiki/Repetitive_strain_injury), but I
nonetheless was intrigued by the unusual
[Kinesis keyboard](http://www.kinesis-ergo.com/contoured.htm), which
some of my collegues at [Relevance](http://thinkrelevance.com) use.

I spent an entire month using the Kinesis. At the end of it, I decided
that it wasn't for me, since I found it somewhat less comfortable and
definitely slower than my crappy $12 keyboard. There was one thing I
*really* liked about it, though, and that was the placement of the
modifier keys: shift, control, and alt. As an
[emacs](http://en.wikipedia.org/wiki/Emacs) user, I frequently type
all sorts of weird combinations of keys, and on the Kinesis these are
generally much easier to do due to the fact that you can use your
thumbs for something other than hitting the spacebar. As a result,
typing things like control-alt-x are two thumbs and a finger.

Of course, I wanted the best of both worlds. I wanted my regular,
non-weird keyboard, but I wanted to be able to type weird combinations
of keys comfortably. If I could somehow physically move the shift,
control, and alt keys to the middle of the keyboard, I'd be there.

What I realized is that this is, in fact, possible. One can use, for
example, j for shift, k for control, and l for alt. The tricky part is
in disambiguating situations where the user wants to simply type these
keys from situations where the user wants to use them to modify other
keys. There are several complications in doing this that I won't bore
you with, but after months of working on it, I finally figured it out.

The cool thing is that along the way I wrote something far more
general than something that only lets you use extra modifier keys.
What I have in khordr is a generalized framework for reacting to key
events at a very low level. This can be used to achieve all sorts of
neat effects, like operating the mouse using your keyboard. That
remains to be implemented, but the supporting code is there and should
make the task relatively straightfoward.

# Installation

khordr currently works only under Windows. I have plans to make it
work for Mac OS X and ideally Linux.

You'll need to install the
[Interception](https://github.com/oblitum/Interception) device driver
and reboot before using this application. NOTE! Having Interception on
your system is technically a security risk, as it allows programs to
intercept everything you type, including passwords. Note that this is
a problem with installing any device driver, not just Interception.

The project is written in [Clojure](http://clojure.org) using
[Leinginen](https://github.com/technomancy/leiningen). Because the
project also contains raw Java compiled by lein's `javac` task, you
will need to correctly configure your setup for this. I'm not 100%
sure what the magic here is, but I've gotten it to work by putting the
following in my `~/.bashrc` under Cygwin:

```bash
export LEIN_JAVA_CMD=~/bin/jdk/bin/java
```

where `~/bin/jdk` is a symlink to
`/cygdrive/c/Program Files (x86)/Java/jdk1.7.0_07/`.
Yes, this is sort of awful. Sorry for that

# Required Hardware

khordr should work with any keyboard, with one caveat: if you make use
of a handler that relies on more than two keys being down
simultaneously, you will probably need a decent keyboard. The feature
you're looking for is called N-Key Rollover, or NKRO. It is common in
gaming keyboards. Less capable keyboards often refuse to report
additional keys once more than two are down, although they are fairly
irregular about it. You may find, for example, that your keyboard is
happy to report n, p, and j down, but won't report n, p, and q.

# Appreciations

Huge thanks to [Francisco Silva](http://oblita.com) for writing
Interception, without which I'd still be crashing my computer trying
to write the device driver part of this myself.
