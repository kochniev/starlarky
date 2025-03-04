package com.verygood.security.larky.modules;

import static com.verygood.security.larky.modules.codecs.TextUtil.HEX_DIGITS;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;


@StarlarkBuiltin(
    name = "jbinascii",
    category = "BUILTIN",
    doc = "The binascii module contains a number of methods to convert between binary and various" +
        " ASCII-encoded binary representations. Normally, you will not use these functions " +
        "directly but use wrapper modules like uu, base64, or binhex instead. " +
        "" +
        "The binascii module contains low-level functions written in C for greater speed that " +
        "are used by the higher-level modules.\n" +
        "\n" +
        "Note: a2b_* functions accept Unicode strings containing only ASCII characters. Other " +
        "functions only accept bytes-like objects (such as bytes, bytearray and other objects " +
        "that support the buffer protocol)."
)
public class BinasciiModule implements StarlarkValue {

  public static final BinasciiModule INSTANCE = new BinasciiModule();
  private static final String NON_HEX_DIGIT_FOUND = "Found non hex digit";
  private static final String ODD_LENGTH_STRING = "String has odd length";

  private byte[] b64decode(byte[] data) throws EvalException {
    try {
        // Mimic CPython's MIME decoder and skip over anything that is not the alphabet
        return Base64.getMimeDecoder().decode(data);
    } catch (IllegalArgumentException e) {
        throw new EvalException(e);
    }
  }

  private int digitValue(char b) throws EvalException {
    if (b >= '0' && b <= '9') {
        return b - '0';
    } else if (b >= 'a' && b <= 'f') {
        return b - 'a' + 10;
    } else if (b >= 'A' && b <= 'F') {
        return b - 'A' + 10;
    } else {
        throw new EvalException(NON_HEX_DIGIT_FOUND);
    }
  }

  @StarlarkMethod(name = "a2b_base64",
      doc = "Convert a block of base64 data back to binary and return the binary data. More " +
          "than one line may be passed at a time.",
      parameters = {
          @Param(
              name = "data",
              allowedTypes = {
                  @ParamType(type=StarlarkBytes.class),
                  @ParamType(type=String.class)}
          )
      },
      useStarlarkThread = true)
  public StarlarkBytes a2b_base64(Object dataO, StarlarkThread thread) throws EvalException {
    byte[] bytes;
    if(dataO instanceof String) {
      bytes = ((String) dataO).getBytes(StandardCharsets.US_ASCII);
    }
    else {
      bytes = ((StarlarkBytes) dataO).toByteArray();
    }
    return StarlarkBytes.of(thread.mutability(), b64decode(bytes));
//
//    return StarlarkBytes.builder(thread)
//        .setSequence(b64decode(bytes))
//        .build();
  }

  //  @Builtin(name = "unhexlify", minNumOfPositionalArgs = 1)
  @StarlarkMethod(name = "a2b_hex",
      doc = "Return the binary data represented by the hexadecimal string hexstr. This function " +
          "is the inverse of b2a_hex(). hexstr must contain an even number of hexadecimal " +
          "digits (which can be upper or lower case), otherwise an Error exception is raised.\n" +
          "\n" +
          "Similar functionality (accepting only text string arguments, but more " +
          "liberal towards whitespace) is also accessible using the " +
          "bytes.fromhex() class method.",
      parameters = {
          @Param(
              name = "hexstr",
              allowedTypes = {
                  @ParamType(type=StarlarkBytes.class),
                  @ParamType(type=String.class)
              }
          )
      },
      useStarlarkThread = true)
  public StarlarkBytes a2b_hex(Object hexed, StarlarkThread thread) throws EvalException {
    String hexstr = "";
    if(hexed instanceof StarlarkBytes) {
      CharsetDecoder decoder = StandardCharsets.US_ASCII.newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);
      try {
        StarlarkBytes byteLike = (StarlarkBytes) hexed;
        hexstr = decoder.decode(ByteBuffer.wrap(byteLike.toByteArray())).toString();
      } catch (CharacterCodingException e) {
        // we accept only ascii-only unicode strings or strings
        throw Starlark.errorf(e.getMessage());
      }
    }
    else if(hexed instanceof String) {
      hexstr = (String) hexed;
    }

    int length = hexstr.length();
    if (length % 2 != 0) {
        throw new EvalException(ODD_LENGTH_STRING);
    }
    byte[] output = new byte[length / 2];
    for (int i = 0; i < length / 2; i++) {
        try {
            output[i] = (byte) (
                (digitValue(hexstr.charAt(i << 1)) << 4) +
                digitValue(hexstr.charAt((i << 1) + 1))
            );
        } catch (NumberFormatException e) {
            throw new EvalException(NON_HEX_DIGIT_FOUND);
        }
    }
    return StarlarkBytes.of(thread.mutability(), output);
//
//    return StarlarkBytes.builder(thread)
//        .setSequence(output)
//        .build();
  }

  //@Builtin(name = "b2a_base64", minNumOfPositionalArgs = 1, numOfPositionalOnlyArgs = 1, parameterNames = {"data"}, keywordOnlyNames = {"newline"})
  @StarlarkMethod(name = "b2a_base64",
      doc = "Return the binary data represented by the hexadecimal string hexstr. This function " +
          "is the inverse of b2a_hex(). hexstr must contain an even number of hexadecimal " +
          "digits (which can be upper or lower case), otherwise an Error exception is raised.\n" +
          "\n" +
          "Similar functionality (accepting only text string arguments, but more " +
          "liberal towards whitespace) is also accessible using the " +
          "bytes.fromhex() class method.",
      parameters = {
          @Param(
              name = "data",
              allowedTypes = {@ParamType(type = StarlarkBytes.class)}
          ),
          @Param(
              name = "newline",
              allowedTypes = {@ParamType(type = Boolean.class)},
              named = true,
              defaultValue = "True"
          )
      },
      useStarlarkThread = true)
  public StarlarkBytes b2a_base64(StarlarkBytes data, Boolean newline, StarlarkThread thread) throws EvalException {
    byte[] encoded;
    try {
      encoded = Base64.getEncoder().encode(data.toByteArray());
    } catch (IllegalArgumentException e) {
        throw new EvalException(e);
    }
    if (newline) {
        encoded = Arrays.copyOf(encoded, encoded.length + 1);
        encoded[encoded.length - 1] = '\n';
    }
    return StarlarkBytes.of(thread.mutability(), encoded);
//
//    return StarlarkBytes.builder(thread)
//            .setSequence(encoded)
//            .build();

  }

//  @Builtin(name = "b2a_hex", minNumOfPositionalArgs = 1)
//  @Builtin(name = "hexlify", minNumOfPositionalArgs = 1)

  @StarlarkMethod(name = "b2a_hex",
      doc = "Return the hexadecimal representation of the binary data. Every byte of data is " +
          "converted into the corresponding 2-digit hex representation. The returned bytes " +
          "object is therefore twice as long as the length of data.\n" +
          "\n" +
          "Similar functionality (but returning a text string) is also conveniently accessible" +
          " using the bytes.hex() method.\n" +
          "\n" +
          "If sep is specified, it must be a single character str or bytes object. It will be" +
          " inserted in the output after every bytes_per_sep input bytes. Separator placement" +
          " is counted from the right end of the output by default, if you wish to count from" +
          " the left, supply a negative bytes_per_sep value.",
      //data[, sep[, bytes_per_sep=1]]
      parameters = {
          @Param(
              name = "data",
              allowedTypes = {@ParamType(type = StarlarkBytes.class)}
          ),
          @Param(
              name = "sep",
              allowedTypes = {
                  @ParamType(type = NoneType.class),
                  @ParamType(type = String.class),
                  @ParamType(type = StarlarkBytes.class)
              },
              named = true,
              defaultValue = "None"
          ),
          @Param(
              name = "bytes_per_sep",
              allowedTypes = {@ParamType(type = StarlarkInt.class)},
              named = true,
              defaultValue = "1"
          )
      }, useStarlarkThread = true)
  public StarlarkBytes b2a_hex(StarlarkBytes binstr, Object sep, StarlarkInt bytes_per_sep, StarlarkThread thread) throws EvalException {
    StringBuilder b = new StringBuilder(binstr.size() * 2);
    byte[] bytes = binstr.toByteArray();
    for (int n : bytes) {
      b.append(HEX_DIGITS[(n >> 4) & 0xF]);
      b.append(HEX_DIGITS[n & 0xF]);
    }
    byte[] encoded = b.toString().toLowerCase().getBytes(StandardCharsets.UTF_8);
    return StarlarkBytes.of(thread.mutability(), encoded);
  }

  static int[] CRC32_TABLE = new int[256];
  static {
    for (int n = 0; n < 256; n++) {
      int c = n;
      for (int k = 8; --k >= 0; ) {
        if ((c & 1) != 0)
          c = 0xEDB88320 ^ (c >>> 1);
        else
          c = c >>> 1;
      }
      CRC32_TABLE[n] = c;
    }
  }

  @StarlarkMethod(
    name = "crc32",
    doc = "Compute CRC-32, the 32-bit checksum of data, starting with an initial CRC of value. " +
            "The default initial CRC is zero. The algorithm is consistent with the ZIP " +
            "file checksum. Since the algorithm is designed for use as a checksum algorithm, " +
            "it is not suitable for use as a general hash algorithm. Use as follows:\n" +
        "\n" +
        "print(binascii.crc32(b\"hello world\"))\n" +
        "# Or, in two pieces:\n" +
        "crc = binascii.crc32(b\"hello\")\n" +
        "crc = binascii.crc32(b\" world\", crc)\n" +
        "print('crc32 = {:#010x}'.format(crc))\n" +
        "Changed in version 3.0: The result is always unsigned." +
        "\n" +
        "To generate the same numeric value across all Python versions and platforms, " +
        "use crc32(data) & 0xffffffff.",
    parameters = {
        @Param(name = "data"),
        @Param(
            name = "value",
            defaultValue = "0",
            allowedTypes = {@ParamType(type = StarlarkInt.class)})
    }
  )
  public StarlarkInt crc32(StarlarkBytes data, StarlarkInt value) throws EvalException {
    int val = 0;
    if(value != StarlarkInt.of(0)) {
      try {
        val = Integer.parseUnsignedInt(value.toString());
      } catch (NumberFormatException e) {
        throw Starlark.errorf("%s must be an integer (0x0 and 0xFFFF_FFFF)!", value);
      }
    }
    byte[] bytes = data.toByteArray();
    int result = ~val;
    for (byte aByte : bytes) {
      result = (result >>> 8) ^ CRC32_TABLE[(result ^ aByte) & 0xff];
    }
    result ^= 0xffffffff;
    return StarlarkInt.of(Integer.toUnsignedLong(result));
  }

}

