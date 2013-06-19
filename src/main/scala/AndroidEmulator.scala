package sbtandroid

import sbt._
import Keys._

import AndroidPlugin._
import AndroidHelpers.isWindows

import complete.DefaultParsers._

object AndroidEmulator {
  private def emulatorStartTask = (parsedTask: TaskKey[String]) =>
    (parsedTask, toolsPath) map { (avd, toolsPath) =>
      "%s/emulator -avd %s".format(toolsPath, avd).run
      ()
    }

  private def listDevicesTask = Def.task { dbPath.value + " devices" !; () }

  private def killAdbTask = Def.task { dbPath.value +" kill-server" !; () }

  private def emulatorStopTask = Def.task {
    streams.value.log.info("Stopping emulators")
    val serial = "%s -e get-serialno".format(dbPath.value).!!
    "%s -s %s emu kill".format(dbPath.value, serial).!
    ()
  }

  def installedAvds(sdkHome: File) = (s: State) => {
    val avds = ((Path.userHome / ".android" / "avd" * "*.ini") +++
      (if (isWindows) (sdkHome / ".android" / "avd" * "*.ini")
       else PathFinder.empty)).get
    Space ~> avds.map(f => token(f.base))
                 .reduceLeftOption(_ | _).getOrElse(token("none"))
  }

  lazy val baseSettings: Seq[Setting[_]] = (Seq(
    listDevices <<= listDevicesTask,
    killAdb <<= killAdbTask,
    emulatorStart <<= InputTask((sdkPath)(installedAvds(_)))(emulatorStartTask),
    emulatorStop <<= emulatorStopTask
  ))

  lazy val aggregateSettings: Seq[Setting[_]] = Seq(
    listDevices,
    emulatorStart,
    emulatorStop
  ) map { aggregate in _ := false }

  lazy val settings: Seq[Setting[_]] = baseSettings ++ aggregateSettings
}