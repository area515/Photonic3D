package org.area515.resinprinter.display.dispmanx;

public enum VC_IMAGE_TYPE_T {
   VC_IMAGE_MIN(0), //bounds for error checking

   VC_IMAGE_RGB565(1),
   VC_IMAGE_1BPP(2),
   VC_IMAGE_YUV420(3),
   VC_IMAGE_48BPP(4),
   VC_IMAGE_RGB888(5),
   VC_IMAGE_8BPP(6),
   VC_IMAGE_4BPP(7),    // 4bpp palettised image
   VC_IMAGE_3D32(8),    /* A separated format of 16 colour/light shorts followed by 16 z values */
   VC_IMAGE_3D32B(9),   /* 16 colours followed by 16 z values */
   VC_IMAGE_3D32MAT(10), /* A separated format of 16 material/colour/light shorts followed by 16 z values */
   VC_IMAGE_RGB2X9(11),   /* 32 bit format containing 18 bits of 6.6.6 RGB, 9 bits per short */
   VC_IMAGE_RGB666(12),   /* 32-bit format holding 18 bits of 6.6.6 RGB */
   VC_IMAGE_PAL4_OBSOLETE(13),     // 4bpp palettised image with embedded palette
   VC_IMAGE_PAL8_OBSOLETE(14),     // 8bpp palettised image with embedded palette
   VC_IMAGE_RGBA32(15),   /* RGB888 with an alpha byte after each pixel */ /* xxx: isn't it BEFORE each pixel? */
   VC_IMAGE_YUV422(16),   /* a line of Y (32-byte padded), a line of U (16-byte padded), and a line of V (16-byte padded) */
   VC_IMAGE_RGBA565(17),  /* RGB565 with a transparent patch */
   VC_IMAGE_RGBA16(18),   /* Compressed (4444) version of RGBA32 */
   VC_IMAGE_YUV_UV(19),   /* VCIII codec format */
   VC_IMAGE_TF_RGBA32(20), /* VCIII T-format RGBA8888 */
   VC_IMAGE_TF_RGBX32(21),  /* VCIII T-format RGBx8888 */
   VC_IMAGE_TF_FLOAT(22), /* VCIII T-format float */
   VC_IMAGE_TF_RGBA16(23), /* VCIII T-format RGBA4444 */
   VC_IMAGE_TF_RGBA5551(24), /* VCIII T-format RGB5551 */
   VC_IMAGE_TF_RGB565(25), /* VCIII T-format RGB565 */
   VC_IMAGE_TF_YA88(26), /* VCIII T-format 8-bit luma and 8-bit alpha */
   VC_IMAGE_TF_BYTE(27), /* VCIII T-format 8 bit generic sample */
   VC_IMAGE_TF_PAL8(28), /* VCIII T-format 8-bit palette */
   VC_IMAGE_TF_PAL4(29), /* VCIII T-format 4-bit palette */
   VC_IMAGE_TF_ETC1(30), /* VCIII T-format Ericsson Texture Compressed */
   VC_IMAGE_BGR888(31),  /* RGB888 with R & B swapped */
   VC_IMAGE_BGR888_NP(32),  /* RGB888 with R & B swapped, but with no pitch, i.e. no padding after each row of pixels */
   VC_IMAGE_BAYER(33),  /* Bayer image, extra defines which variant is being used */
   VC_IMAGE_CODEC(34),  /* General wrapper for codec images e.g. JPEG from camera */
   VC_IMAGE_YUV_UV32(35),   /* VCIII codec format */
   VC_IMAGE_TF_Y8(36),   /* VCIII T-format 8-bit luma */
   VC_IMAGE_TF_A8(37),   /* VCIII T-format 8-bit alpha */
   VC_IMAGE_TF_SHORT(38),/* VCIII T-format 16-bit generic sample */
   VC_IMAGE_TF_1BPP(39), /* VCIII T-format 1bpp black/white */
   VC_IMAGE_OPENGL(40),
   VC_IMAGE_YUV444I(41), /* VCIII-B0 HVS YUV 4:4:4 interleaved samples */
   VC_IMAGE_YUV422PLANAR(42),  /* Y, U, & V planes separately (VC_IMAGE_YUV422 has them interleaved on a per line basis) */
   VC_IMAGE_ARGB8888(43),   /* 32bpp with 8bit alpha at MS byte, with R, G, B (LS byte) */
   VC_IMAGE_XRGB8888(44),   /* 32bpp with 8bit unused at MS byte, with R, G, B (LS byte) */

   VC_IMAGE_YUV422YUYV(45),  /* interleaved 8 bit samples of Y, U, Y, V */
   VC_IMAGE_YUV422YVYU(46),  /* interleaved 8 bit samples of Y, V, Y, U */
   VC_IMAGE_YUV422UYVY(47),  /* interleaved 8 bit samples of U, Y, V, Y */
   VC_IMAGE_YUV422VYUY(48),  /* interleaved 8 bit samples of V, Y, U, Y */

   VC_IMAGE_RGBX32(49),      /* 32bpp like RGBA32 but with unused alpha */
   VC_IMAGE_RGBX8888(50),    /* 32bpp, corresponding to RGBA with unused alpha */
   VC_IMAGE_BGRX8888(51),    /* 32bpp, corresponding to BGRA with unused alpha */

   VC_IMAGE_YUV420SP(52),    /* Y as a plane, then UV byte interleaved in plane with with same pitch, half height */

   VC_IMAGE_YUV444PLANAR(53),  /* Y, U, & V planes separately 4:4:4 */

   VC_IMAGE_TF_U8(54),   /* T-format 8-bit U - same as TF_Y8 buf from U plane */
   VC_IMAGE_TF_V8(55),   /* T-format 8-bit U - same as TF_Y8 buf from V plane */

   VC_IMAGE_MAX(56),     //bounds for error checking
   VC_IMAGE_FORCE_ENUM_16BIT(0xffff);

   private int cIndex;

   VC_IMAGE_TYPE_T(int cIndex) {
	   this.cIndex = cIndex;
   }

   public int getcIndex() {
	   return cIndex;
   }
}