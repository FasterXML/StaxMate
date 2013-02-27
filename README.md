1. StaxMate -- the perfect companion for a Stax processor!

StaxMate is an Open Source helper library (or, mini-framework), written in Java, designed to simplify common tasks for which Stax-based streaming pull parsers are used (and good) for.
This simplification, and added convenience is to be achieved without compromising positive performance characteristics of good Stax implementations, such as low memory usage and high processing throughput.

As with Stax API, input and output sides are separate, and distinction may be even more pronounced.
However, whereas Stax API has division between "raw" cursor API, and more object-oriented Event API, StaxMate presents only a single level of abstraction, on input and output sides.
StaxMate reader-side abstractions are mostly based on Cursor API, but some support for Event API is also planned for interoperability purposes.

2. Stax implementations supported

StaxMate aims to be implementation dependant, so in theory any compliant Stax implementation should be usable.
In practice, some implementations may work better than others -- you may want to check out Stax-related email lists for comments on which implementations wor best.

As of version 1.2, following implementations are known to work well enough to pass StaxMate unit tests:

* Woodstox [http://woodstox.codehaus.org]
* Sun SJSXP [http://sjsxp.dev.java.net]
* Aalto (experimental) [http://www.cowtowncoder.com/hatchery/aalto/index.html]

Since the initial development was done against Woodstox Stax processor, Woodstox is probably least likely to have problems with StaxMate.


3. Licensing

StaxMate is licensed according to terms in 'LICENSE' file, found with the distribution: "binary" distributions having a single specific license, and source distributions possibly including multiple ones from which to choose one to use.

Contributions to the source code need to be made licensable under _all_ the licenses that the source distribution allows: this to ensure that StaxMate can continue distributed under these licenses, to maximize usefulness to all Open Source developers.


4. Authors

Author of StaxMate is Tatu Saloranta, tatu.saloranta@iki.fi

Other people who have participated in development (usually by submitting bug reports and patches) are listed in 'CREDITS' file.
They are considered co-authors of StaxMate, and are integral to the quality of the codebase.
