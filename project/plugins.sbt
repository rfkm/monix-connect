addSbtPlugin("org.scalariform"      % "sbt-scalariform"     % "1.8.2")
addSbtPlugin("com.typesafe.sbt"     % "sbt-native-packager" % "1.3.4")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.3.2")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.25")
addSbtPlugin("com.typesafe.sbt"     % "sbt-git"         % "1.0.0")
addSbtPlugin("com.github.tkawachi"  % "sbt-doctest"     % "0.9.6")
addSbtPlugin("com.eed3si9n"         % "sbt-unidoc"      % "0.4.3")
addSbtPlugin("com.typesafe"         % "sbt-mima-plugin" % "0.6.4")
addSbtPlugin("de.heikoseeberger"    % "sbt-header"      % "5.4.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.2")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"   % "1.6.1")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.1"

