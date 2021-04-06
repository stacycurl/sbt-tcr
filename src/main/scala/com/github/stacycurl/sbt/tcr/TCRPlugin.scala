package com.github.stacycurl.sbt.tcr

import java.io.File
import com.typesafe.sbt.GitPlugin.autoImport.git
import com.typesafe.sbt.git.{GitRunner, NullLogger}
import sbt.Def.{task, taskDyn}
import sbt.Keys.{baseDirectory, compile, streams, test}
import sbt.{AutoPlugin, Def, Logger, PluginTrigger, Plugins, SettingKey, Task, TaskKey, Test, file, taskKey}


object TCRPlugin extends AutoPlugin {
  override def requires: Plugins = sbt.plugins.JvmPlugin
  override def trigger: PluginTrigger = allRequirements

  object autoImport {
    val tcrTestCommitOrRevert = taskKey[Unit]("test && commit || revert")
  
    val tcrTestAppendOrRevert = taskKey[Unit]("test && append || revert")
    
    val tcrCompileCommitOrRevert = taskKey[Unit]("compile && commit || revert")
    
    val tcrCompileAppendOrRevert = taskKey[Unit]("compile && append || revert")
  }
  
  import autoImport._

  val append    = List("commit", "--amend", "--no-edit") 
  val commitDot = List("commit", "-m", ".")
  
  override lazy val projectSettings = Seq(
    tcrTestCommitOrRevert := taskDyn(TCR(Test / test, baseDirectory, commitDot, streams.value.log).doIt),
    tcrTestAppendOrRevert := taskDyn(TCR(Test / test, baseDirectory, append, streams.value.log).doIt),

    tcrCompileCommitOrRevert := taskDyn(TCR(Test / compile, baseDirectory, commitDot, streams.value.log).doIt),
    tcrCompileAppendOrRevert := taskDyn(TCR(Test / compile, baseDirectory, append, streams.value.log).doIt)
  )
}

case class TCR[A](verify: TaskKey[A], dir: SettingKey[File], commitCommand: List[String], logger: Logger) {
  lazy val doIt: Def.Initialize[Task[Unit]] = taskDyn[Unit] {
    if (gitHasUncommittedChanges.value) Def.sequential(
      addChanges,
      taskDyn {
        if (verify.result.value.toEither.isRight) commit else restore
      }
    ) else task {
      logger.info(s"No changes, nothing to ${verify.key.label}")
    }
  }

  val gitHasUncommittedChanges: Def.Initialize[Task[Boolean]] = task {
    val dirValue: File = dir.value
    
    val rootPath = file(".").getAbsolutePath.stripSuffix("/.")

    val inProjectRoot: Boolean =
      rootPath == dirValue.getAbsolutePath
    
    logger.debug(s"Checking for changes in: ${dirValue.getName}")
    
    val runner: GitRunner = git.runner.value

    val uncommittedChanges: Seq[String] = for {
      command <- Seq(
        Seq("diff-index", "--cached", "HEAD"),
        Seq("diff-index", "HEAD"),
        Seq("diff-files"),
        Seq("ls-files", "--exclude-standard", "--others")
      )
      changes = runner(command: _*)(dir.value, NullLogger)
      if changes.nonEmpty
      last    <- changes.split("\\s").lastOption
      if inProjectRoot || last.startsWith(dirValue.getName)
    } yield last
    
    uncommittedChanges.exists(_.nonEmpty)
  }

  val addChanges: Def.Initialize[Task[Unit]] = task[Unit] {
    logger.debug("Adding changes")
    
    git.runner.value.apply("add", ".")(dir.value, logger)
  }
  
  val commit: Def.Initialize[Task[Unit]] = task[Unit] {
    logger.info("Commiting changes")
  
    git.runner.value.apply(commitCommand: _*)(dir.value, logger)
  }
  
  val restore: Def.Initialize[Task[Unit]] = task[Unit] {
    logger.info("restoring.")

    git.runner.value.apply("reset", ".")(dir.value, logger)
    git.runner.value.apply("checkout", "--", ".")(dir.value, logger)
  }
}

