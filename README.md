# StaxMate -- the perfect companion for a Stax processor!

StaxMate is an Open Source helper library (or, mini-framework), written in Java, designed to simplify common tasks for which Stax-based streaming pull parsers are used (and good) for.
This simplification, and added convenience is to be achieved without compromising positive performance characteristics of good Stax implementations, such as low memory usage and high processing throughput.

As with Stax API, input and output sides are separate, and distinction may be even more pronounced.
However, whereas Stax API has division between "raw" cursor API, and more object-oriented Event API, StaxMate presents only a single level of abstraction, on input and output sides.
StaxMate reader-side abstractions are mostly based on Cursor API, but some support for Event API is also planned for interoperability purposes.

## Status

[![Build Status](https://travis-ci.org/FasterXML/StaxMate.svg)](https://travis-ci.org/FasterXML/StaxMate)

# Stax implementations supported

StaxMate aims to be implementation dependant, so in theory any compliant Stax implementation should be usable.
In practice, some implementations may work better than others -- you may want to check out Stax-related email lists for comments on which implementations wor best.

As of version 1.2, following implementations are known to work well enough to pass StaxMate unit tests:

 * [Woodstox](http://woodstox.codehaus.org)
 * [Sun SJSXP](http://sjsxp.dev.java.net)
 * [Aalto](https://github.com/FasterXML/aalto-xml)

Since the initial development was done against Woodstox Stax processor, Woodstox is probably least likely to have problems with StaxMate.
There are also some known issue with SJSXP: specifically, it tends to write unnecessary namespace declarations.

# Licensing

StaxMate is licensed under [BSD 2](LICENSE.txt)

# Documentation, downloads

Check [project Wiki](../../wiki).

# Authors

Author of StaxMate is Tatu Saloranta, tatu.saloranta@iki.fi

Other people who have participated in development (usually by submitting bug reports and patches) are listed in 'CREDITS' file.
They are considered co-authors of StaxMate, and are integral to the quality of the codebase.
