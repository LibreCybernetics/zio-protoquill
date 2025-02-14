package io.getquill.context.jdbc.postgres

import java.time.LocalDateTime

import io.getquill.context.sql.EncodingSpec
import io.getquill._
import java.time.ZoneId

class JdbcEncodingSpec extends EncodingSpec {

  val context: testContext.type = testContext
  import testContext._

  "encodes and decodes types" in {
    // Why is this a dynamic query??? Does this issue happen for all deletes?
    testContext.run(delete)
    testContext.run(liftQuery(insertValues).foreach(e => insert(e)))
    verify(testContext.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testContext.run(query[EncodingTestEntity].delete)
    testContext.run(liftQuery(insertValues).foreach(e => query[EncodingTestEntity].insertValue(e)))
    val q = quote {
      (set: Query[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q(liftQuery(insertValues.map(_.v6)))))
  }

  "returning custom type" in {
    val uuid = testContext.run(insertBarCode(lift(barCodeEntry))).get
    val (barCode :: Nil) = testContext.run(findBarCodeByUuid(uuid))

    verifyBarcode(barCode)
  }

  // IO Monad not working yet so need to do regular queries
  "LocalDateTime" in {
    case class EncodingTestEntity(v11: Option[LocalDateTime])
    val now = LocalDateTime.now()
    val e1 = EncodingTestEntity(Some(now))
    val e2 = EncodingTestEntity(None)
    val res: (List[EncodingTestEntity], List[EncodingTestEntity]) = {
      val steps = {
        testContext.run(query[EncodingTestEntity].delete)
        testContext.run(query[EncodingTestEntity].insertValue(lift(e1)))
        val withoutNull = testContext.run(query[EncodingTestEntity])
        testContext.run(query[EncodingTestEntity].delete)
        testContext.run(query[EncodingTestEntity].insertValue(lift(e2)))
        val withNull = testContext.run(query[EncodingTestEntity])
        (withoutNull, withNull)
      }
      steps
    }
    res._1 must contain theSameElementsAs List(EncodingTestEntity(Some(now)))
    res._2 must contain theSameElementsAs List(EncodingTestEntity(None))
  }

  "Encode/Decode Other Time Types" in {
    context.run(query[TimeEntity].delete)
    val zid = ZoneId.systemDefault()
    val timeEntity = TimeEntity.make(zid)
    context.run(query[TimeEntity].insertValue(lift(timeEntity)))
    val actual = context.run(query[TimeEntity]).head
    timeEntity mustEqual actual
  }
}
