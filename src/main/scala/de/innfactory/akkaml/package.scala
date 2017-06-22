package de.innfactory

import scala.reflect.{ ClassTag, classTag }

package object akkaml {

  type Traversable[+A] = scala.collection.immutable.Traversable[A]
  type Iterable[+A] = scala.collection.immutable.Iterable[A]
  type Seq[+A] = scala.collection.immutable.Seq[A]
  type IndexedSeq[+A] = scala.collection.immutable.IndexedSeq[A]

  def className[A: ClassTag]: String = classTag[A].runtimeClass.getName
}
