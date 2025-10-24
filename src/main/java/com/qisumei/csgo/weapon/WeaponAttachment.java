package com.qisumei.csgo.weapon;

/**
 * 武器附件类 - 定义武器可用的附件（如瞄准镜等）
 */
public class WeaponAttachment {
    private final String attachmentId;
    private final String displayName;
    private final AttachmentType type;

    public WeaponAttachment(String attachmentId, String displayName, AttachmentType type) {
        this.attachmentId = attachmentId;
        this.displayName = displayName;
        this.type = type;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AttachmentType getType() {
        return type;
    }

    /**
     * 附件类型枚举
     */
    public enum AttachmentType {
        SCOPE("瞄准镜"),
        BARREL("枪管"),
        GRIP("握把"),
        STOCK("枪托"),
        MAGAZINE("弹匣");

        private final String displayName;

        AttachmentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 预定义的常用附件
    public static final WeaponAttachment ACOG_SCOPE = new WeaponAttachment(
        "pointblank:acog", "ACOG瞄准镜", AttachmentType.SCOPE
    );

    public static final WeaponAttachment SCOPE_8X = new WeaponAttachment(
        "pointblank:scope_x8", "8倍镜", AttachmentType.SCOPE
    );
}
