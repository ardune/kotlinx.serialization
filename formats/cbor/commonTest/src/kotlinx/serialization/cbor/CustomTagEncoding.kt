/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class CustomTagEncoding{

    @Serializable
    data class Something(
        val someTaggedByteString: TaggedByteString?
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    @Serializable(with = TaggedByteStringSerializer::class)
    data class TaggedByteString(
        val data: ByteArray,
    )

    @ExperimentalUnsignedTypes
    @ExperimentalSerializationApi
    class TaggedByteStringSerializer : KSerializer<TaggedByteString> {
        override val descriptor: SerialDescriptor
            get() = SerialDescriptor("TaggedByteString", ByteArraySerializer().descriptor)

        override fun deserialize(decoder: Decoder): TaggedByteString {
            if (decoder is CborDecoder) {
                // you can ignore these checks if you don't care
                val tags = decoder.processTags()
                checkNotNull(tags) {
                    "expected TaggedByteString to be tagged"
                }
                check(tags[0] == 42.toULong()) {
                    "TaggedByteString should be tagged with 42"
                }
            }
            return TaggedByteString(decoder.decodeSerializableValue(ByteArraySerializer()))
        }

        override fun serialize(
            encoder: Encoder,
            value: TaggedByteString,
        ) {
            if (encoder is CborEncoder) {
                encoder.encodeTag(42u)
            }
            encoder.encodeSerializableValue(ByteArraySerializer(), value.data)
        }
    }

    @Test
    fun withSerializerValue() {
        val cbor = Cbor {
            useDefiniteLengthEncoding = true
        }

        val target = Something(
            someTaggedByteString = null
        )

        /**
         * A1                                      # map(1)
         *    74                                   # text(20)
         *       736F6D6554616767656442797465537472696E67 # "someTaggedByteString"
         *    F6                                   # primitive(22) <- no tag here!
         */
        println(cbor.encodeToHexString(target))

        val target2 = Something(
            someTaggedByteString = TaggedByteString(
                byteArrayOf(0x01, 0x02, 0x03)
            )
        )

        /**
         * A1                                      # map(1)
         *    74                                   # text(20)
         *       736F6D6554616767656442797465537472696E67 # "someTaggedByteString"
         *    D8 2A                                # tag(42) <- the tag
         *       83                                # array(3)
         *          01                             # unsigned(1)
         *          02                             # unsigned(2)
         *          03                             # unsigned(3)
         *
         */
        println(cbor.encodeToHexString(target2))

        // take it or leave it, you can verify things if it is critical to your app
        /**
         * A1                                      # map(1)
         *    74                                   # text(20)
         *       736F6D6554616767656442797465537472696E67 # "someTaggedByteString"
         *                                          <- note the absence of the tag
         *    83                                   # array(3)
         *       01                                # unsigned(1)
         *       02                                # unsigned(2)
         *       03                                # unsigned(3)
         *
         */
        assertFails {
            cbor.decodeFromHexString<Something>("a174736f6d6554616767656442797465537472696e6783010203")
        }
    }
}