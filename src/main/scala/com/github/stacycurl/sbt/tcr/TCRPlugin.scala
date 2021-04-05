package com.github.stacycurl.sbt.tcr

import java.io.File
import com.typesafe.sbt.GitPlugin.autoImport.git
import com.typesafe.sbt.git.{GitRunner, NullLogger}
import sbt.Keys.{baseDirectory, compile, test}
import sbt.{AutoPlugin, Def, SettingKey, Task, TaskKey, Test, file, taskKey}

object TCRPlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    val tcrTestCommitOrRevert = taskKey[Unit]("test && commit || revert")
  
    val tcrTestAppendOrRevert = taskKey[Unit]("test && append || revert")
    
    val tcrCompileCommitOrRevert = taskKey[Unit]("compile && commit || revert")
    
    val tcrCompileAppendOrRevert = taskKey[Unit]("compile && append || revert")
  }
  
  import autoImport._

  val append     = List("commit", "--amend", "--no-edit") 
  val commitDot = List("commit", "-m", ".")
  
  override lazy val projectSettings = Seq(
    tcrTestCommitOrRevert := TCR(Test / test, baseDirectory, commitDot).task.value,
    tcrTestAppendOrRevert := TCR(Test / test, baseDirectory, append).task.value,

    tcrCompileCommitOrRevert := TCR(Test / compile, baseDirectory, commitDot).task.value,
    tcrCompileAppendOrRevert := TCR(Test / compile, baseDirectory, append).task.value
  )
}

case class TCR[A](verify: TaskKey[A], dir: SettingKey[File], commitCommand: List[String]) {
  lazy val task: Def.Initialize[Task[Unit]] = {
    Def.taskDyn[Unit] {
      if (gitHasUncommittedChanges.value) Def.sequential(
        addChanges,
        Def.taskDyn {
          if (verify.result.value.toEither.isRight) commit else restore
        }
      ) else Def.task {
        log(s"No changes, nothing to ${verify.key.label}")
      }
    }
  }
  
  val gitHasUncommittedChanges: Def.Initialize[Task[Boolean]] = Def.task {
    val dirValue: File = dir.value
    
    val rootPath = file(".").getAbsolutePath.stripSuffix("/.")

    val inProjectRoot: Boolean =
      rootPath == dirValue.getAbsolutePath
    
//    log(s".   = ${rootPath}")
//    log(s"dir = ${dirValue.getAbsolutePath}")
    log(s"Checking for changes in: ${dirValue.getName}")
    
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

  def addChanges: Def.Initialize[Task[Unit]] = Def.task[Unit] {
    log("Adding changes")
    
    git.runner.value.apply("add", ".")(dir.value, sbt.Keys.streams.value.log)
  }
  
  val commit: Def.Initialize[Task[Unit]] = Def.task[Unit] {
    log("Commiting changes")
  
   git.runner.value.apply(commitCommand: _*)(dir.value, sbt.Keys.streams.value.log)
  }
  
  
  val restore: Def.Initialize[Task[Unit]] = Def.task[Unit] {
    log("restoring.")

    git.runner.value.apply("reset", ".")(dir.value, sbt.Keys.streams.value.log)
    git.runner.value.apply("checkout", "--", ".")(dir.value, sbt.Keys.streams.value.log)
  }
  
  private def log[Z](value: Z): Unit = {
    println(value.toString)
  }
}

