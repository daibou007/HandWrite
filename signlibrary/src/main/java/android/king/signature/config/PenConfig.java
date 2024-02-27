package android.king.signature.config;


import android.graphics.Color;


/**
 * 画笔配置
 *
 * @author king
 * @since 2018/06/15
 */
public class PenConfig {

    /**
     * 画笔大小等级
     */
    public static int PAINT_SIZE_LEVEL = 2;

    /**
     * 画笔颜色
     */
    public static int PAINT_COLOR = Color.parseColor("#101010");

    /**
     * 笔锋控制值,越小笔锋越粗,越不明显
     */
    public static final float DIS_VEL_CAL_FACTOR = 0.008f;


    /**
     * 主题颜色
     */
    public static int THEME_COLOR = Color.parseColor("#3b98ed");//3b98ed

    public static final String SAVE_PATH = "path";

    /**
     * jpg格式
     */
    public static final String FORMAT_JPG = "JPG";
    /**
     * png格式
     */
    public static final String FORMAT_PNG = "PNG";

}
