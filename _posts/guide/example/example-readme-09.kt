// This file was automatically generated from 2021-11-28-continuation-monad-in-kotlin.md by Knit tool. Do not edit.
package example.exampleReadme09

import arrow.*
import arrow.core.*
import arrow.fx.coroutines.*
import kotlinx.coroutines.*
import io.kotest.matchers.collections.*
import io.kotest.assertions.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import kotlin.coroutines.cancellation.CancellationException
import io.kotest.property.*
import io.kotest.property.arbitrary.*
import arrow.core.test.generators.*

import java.io.*

@JvmInline
value class Content(val body: List<String>)

sealed interface FileError
@JvmInline value class SecurityError(val msg: String?) : FileError
@JvmInline value class FileNotFound(val path: String) : FileError
object EmptyPath : FileError {
  override fun toString() = "EmptyPath"
}

fun readFile(path: String?): Cont<FileError, Content> = cont {
  ensureNotNull(path) { EmptyPath }
  ensure(path.isNotEmpty()) { EmptyPath }
  try {
    val lines = File(path).readLines()
    Content(lines)
  } catch (e: FileNotFoundException) {
    shift(FileNotFound(path))
  } catch (e: SecurityException) {
    shift(SecurityError(e.message))
  }
}

suspend fun <A> awaitExitCase(exit: CompletableDeferred<ExitCase>): A =
  guaranteeCase(::awaitCancellation) { exitCase -> exit.complete(exitCase) }

suspend fun test() {
  val exit = CompletableDeferred<ExitCase>()
  cont<FileError, Int> {
    withContext(Dispatchers.IO) {
      val job = launch { awaitExitCase(exit) }
      val content = readFile("failure").bind()
      job.join()
      content.body.size
    }
  }.fold({ e -> e shouldBe FileNotFound("failure") }, { fail("Int can never be the result") })
  exit.await().shouldBeInstanceOf<ExitCase>()
}
