package net.djvk.inkyPhotoPrep

import net.djvk.inkyPhotoPrep.lib.BinaryUtilities
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

internal class BinaryUtilitiesTest {
    companion object {
        @JvmStatic
        fun provide_getPowerOfTwo(): List<Arguments> {
            return listOf(
                Arguments.of(
//                    testName: String,
                    "1",
//                    input: Int,
                    1,
//                    expectedOutput: Int,
                    0,
                ),
                Arguments.of(
//                    testName: String,
                    "2",
//                    input: Int,
                    2,
//                    expectedOutput: Int,
                    1,
                ),
                Arguments.of(
//                    testName: String,
                    "16",
//                    input: Int,
                    16,
//                    expectedOutput: Int,
                    4,
                ),
                Arguments.of(
//                    testName: String,
                    "2^20",
//                    input: Int,
                    1048576,
//                    expectedOutput: Int,
                    20,
                ),
                Arguments.of(
//                    testName: String,
                    "5",
//                    input: Int,
                    5,
//                    expectedOutput: Int,
                    null,
                ),
            )
        }
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provide_getPowerOfTwo")
    fun test_getPowerOfTwo(
        testName: String,
        input: Int,
        expectedOutput: Int?,
    ) {
        assertEquals(expectedOutput, BinaryUtilities.getPowerOfTwo(input))
    }
}