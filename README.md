[![Build Status][badge-build]][link-build]
[![Maven Central][badge-maven]][link-maven]

# sbt-tcr
sbt> test &amp;&amp; commit || revert

Add the following to `project/plugins.sbt`

```scala
addSbtPlugin("com.github.stacycurl" % "sbt-tcr" % "1.0.0")
```

Resources:

https://medium.com/@kentbeck_7670/test-commit-revert-870bbd756864


[badge-build]: https://github.com/stacycurl/sbt-tcr/actions/workflows/build.yml/badge.svg
[link-build]: https://github.com/stacycurl/sbt-tcr/actions/

[badge-maven]: https://maven-badges.herokuapp.com/maven-central/com.github.stacycurl/sbt-tcr_2.12_1.0/badge.svg
[link-maven]: https://maven-badges.herokuapp.com/maven-central/com.github.stacycurl/sbt-tcr_2.12_1.0