package scalona.monix.connect.s3

import java.nio.ByteBuffer

import monix.eval.Task
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.services.s3.model.{CompleteMultipartUploadResponse, PutObjectResponse}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.{Consumer, Observable}
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.util.{Failure, Success, Try}

class S3AsyncSpec
  extends AnyWordSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures with S3Fixture with Eventually {

  private val bucketName = "sample-bucket"
  implicit val s3Client = s3AsyncClient

  override implicit val patienceConfig = PatienceConfig(10.seconds, 100.milliseconds)

  s"${S3}" should {

    "implement putObject method" that {

      s"uploads the passed ByteBuffer to the respective s3 bucket and key" when {

        "contentLength and contentType are not defined and therefore infered by the method" in {
          //given
          val key = Gen.alphaLowerStr.sample.get
          val content = Gen.alphaUpperStr.sample.get

          //when
          val t: Task[PutObjectResponse] = S3.putObject(bucketName, key, ByteBuffer.wrap(content.getBytes()))

          //then
          whenReady(t.runToFuture) { putResponse =>
            val s3Object: ByteBuffer = S3.getObject(bucketName, key).runSyncUnsafe()
            putResponse shouldBe a[PutObjectResponse]
            s3Object.array() shouldBe content.getBytes()
          }
        }

        "contentLength and contentType are defined respectively as the array lenght and 'application/json'" in {
          //given
          val key = Gen.alphaLowerStr.sample.get
          val content = Gen.alphaUpperStr.sample.get

          //when
          val t: Task[PutObjectResponse] = S3.putObject(
            bucketName,
            key,
            ByteBuffer.wrap(content.getBytes()),
            Some(content.length),
            Some("appliction/json"))

          //then
          whenReady(t.runToFuture) { putResponse =>
            val s3Object: ByteBuffer = S3.getObject(bucketName, key).runSyncUnsafe()
            putResponse shouldBe a[PutObjectResponse]
            s3Object.array() shouldBe content.getBytes()
          }
        }
      }
    }

    "correctly perform a multipart upload" when {

      "the observable contain a single ByteBuffer" in {
        //given
        val key = Gen.alphaLowerStr.sample.get
        val content = ByteBuffer.wrap(Gen.alphaUpperStr.sample.get.getBytes)
        val stream = Observable.fromIterable(Seq(content))

        //when
        val t: Task[CompleteMultipartUploadResponse] = S3.multipartUpload(bucketName, key, stream)

        //then
        whenReady(t.runToFuture) { completeMultipartUpload =>
          val s3Object: ByteBuffer = S3.getObject(bucketName, key).runSyncUnsafe()
          s3SyncClient.doesObjectExist(bucketName, key) shouldBe true
          completeMultipartUpload shouldBe a[CompleteMultipartUploadResponse]
          s3Object.array() shouldBe content.array()
        }
      }

      "the observable of chunks is consumed with multipartUploadConsumer" in {
        //given
        val key = Gen.alphaLowerStr.sample.get
        val content: Array[Byte] = Gen.alphaUpperStr.sample.get.getBytes
        val consumer: Consumer[Array[Byte], Task[CompleteMultipartUploadResponse]] =
          S3.multipartUploadConsumer(bucketName, key)
        val ob = Observable.fromIterable(Seq(content))

        //when
        val t: Task[CompleteMultipartUploadResponse] = ob.consumeWith(consumer).flatten

        //then
        whenReady(t.runToFuture) { completeMultipartUpload =>
          val s3Object: ByteBuffer = S3.getObject(bucketName, key).runSyncUnsafe()
          s3SyncClient.doesObjectExist(bucketName, key) shouldBe true
          completeMultipartUpload shouldBe a[CompleteMultipartUploadResponse]
          s3Object.array() shouldBe content
        }
      }

      /* "the observable of chunks is consumed with multipartUploadConsumer" in {
        //given
        val key = Gen.alphaLowerStr.sample.get
        val contentList: Seq[Array[Byte]] =
          Gen.listOfN(1, Gen.alphaStr).sample.get.map(_.getBytes)
        val stream = Observable.fromIterable(contentList)

        //when
        val t: Task[CompleteMultipartUploadResponse] = S3.multipartUpload(bucketName, key, stream, Some("application/octet-stream"))

        //then
        whenReady(t.runToFuture) { completeMultipartUpload =>
          val s3Object: ByteBuffer = S3.getObject(bucketName, key).runSyncUnsafe()
          s3SyncClient.doesObjectExist(bucketName, key) shouldBe true
          completeMultipartUpload shouldBe a[CompleteMultipartUploadResponse]
          s3Object.array() shouldBe contentList.map(_.array()).flatten
        }
      }*/
    }

    "download a ByteBuffer of an existing s3 object" in {
      //given
      val key = Gen.alphaLowerStr.sample.get
      val content = Gen.alphaUpperStr.sample.get
      s3SyncClient.putObject(bucketName, key, content)

      //when
      val t: Task[ByteBuffer] = S3.getObject(bucketName, key)

      whenReady(t.runToFuture) { actualContent: ByteBuffer =>
        s3SyncClient.doesObjectExist(bucketName, key) shouldBe true
        actualContent shouldBe a[ByteBuffer]
        actualContent.array() shouldBe content.getBytes()
      }
    }
  }

  def genPart() = Gen.oneOf(Seq(List.fill(1000)(Gen.alphaStr.sample.get).mkString("_")))

  override def beforeAll(): Unit = {
    super.beforeAll()
    Try(s3SyncClient.createBucket(bucketName)) match {
      case Failure(exception) =>
        info(s"The attempt to create bucket $bucketName failed since it already existed, exception: $exception")
      case Success(_) => info(s"Bucket $bucketName created successfully")
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
    //s3SyncClient.deleteBucket(bucketName)
  }
}
