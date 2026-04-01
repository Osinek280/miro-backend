package com.example.miro.board.utils;

import com.example.miro.board.entities.Point;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class WireCodec {
  private static final double POINT_SCALE = 1000.0;

  private WireCodec() {}

  public static List<Point> decodePoints(String base64) {
    if (base64 == null || base64.isBlank()) return List.of();

    byte[] bytes = Base64.getDecoder().decode(base64);

    // każdy punkt to 2x Int32 = 8 bajtów
    if (bytes.length == 0 || bytes.length % 8 != 0) return List.of();

    ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    int count = bytes.length / 8;
    List<Point> points = new ArrayList<>(count);

    int accumX = 0;
    int accumY = 0;

    for (int i = 0; i < count; i++) {
      accumX += buf.getInt();
      accumY += buf.getInt();
      points.add(new Point(accumX / POINT_SCALE, accumY / POINT_SCALE));
    }

    return points;
  }
}
