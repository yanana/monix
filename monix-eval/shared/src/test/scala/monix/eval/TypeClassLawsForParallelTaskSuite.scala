/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.eval

import cats.{Applicative, Eq, Functor, Monad}
import cats.kernel.laws.GroupLaws
import cats.laws.discipline.{CoflatMapTests, MonadErrorTests}
import cats.syntax.all._
import monix.execution.schedulers.TestScheduler
import org.scalacheck.Arbitrary

import scala.util.Success
import scala.concurrent.duration._
import monix.eval.Task.nondeterminism

object TypeClassLawsForParallelTaskSuite extends BaseLawsSuite
  with GroupLaws[Task[Int]]  {

  // for GroupLaws
  override def Equ: Eq[Task[Int]] = equalityTask[Int]
  override def Arb: Arbitrary[Task[Int]] = arbitraryTask[Int]

  // Actual tests ...

  checkAll("Group[ParallelTask[Int]]", GroupLaws[Task[Int]].group)
  checkAll("Monoid[ParallelTask[Int]]", GroupLaws[Task[Int]].monoid)
  checkAll("Semigroup[ParallelTask[Int]]", GroupLaws[Task[Int]].semigroup)

  checkAll("MonadError[ParallelTask[Int]]", MonadErrorTests[Task, Throwable].monadError[Int,Int,Int])
  checkAll("CoflatMap[ParallelTask[Int]]", CoflatMapTests[Task].coflatMap[Int,Int,Int])

  test("ParallelTask should execute in parallel") {
    assertEquals(implicitly[Functor[Task]], nondeterminism)
    assertEquals(implicitly[Applicative[Task]], nondeterminism)
    assertEquals(implicitly[Monad[Task]], nondeterminism)

    implicit val s = TestScheduler()

    val task1 = Task(1).delayExecution(1.second)
    val task2 = Task(2).delayExecution(1.second)
    val both = (task1 |@| task2).map(_ + _)

    val f = both.runAsync
    s.tick(1.second)
    assertEquals(f.value, Some(Success(3)))
  }
}