package com.ncr.ssco.communication.hook;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by stefanobertarello on 28/02/17.
 */
public enum FieldTypes {
    BYTE,
    I1,
    BOOLEAN,
    UNSIGNED_BYTE,
    I2,
    SHORT,
    UNSIGNED_SHORT,
    I4,
    INT,
    LONG,
    UNSIGNED_LONG,
    STRING,
    L4STRING,
    BIN_BASE64;

    private static final Map<String, FieldTypes> map;
    private static final Map<FieldTypes, String> names;
    private static final Map<FieldTypes, Class> classes;
    private static final Map<Class, FieldTypes> valueTypes;

    FieldTypes() {
    }

    public String toString() {
        return (String)names.get(this);
    }

    public static FieldTypes parse(String sName) {
        if(sName == null) {
            return STRING;
        } else if(sName.isEmpty()) {
            return STRING;
        } else {
            FieldTypes type = (FieldTypes)map.get(sName);
            if(type == null) {
                throw new RuntimeException("Unknown data type:  " + sName);
            } else {
                return type;
            }
        }
    }

    public static Class getClass(FieldTypes type) {
        if(type == null) {
            throw new RuntimeException("Unknown type");
        } else {
            Class clazz = (Class)classes.get(type);
            if(clazz == null) {
                throw new RuntimeException("Unknown type:  " + type);
            } else {
                return clazz;
            }
        }
    }

    public static String getTypeName(Class clazz) {
        if(clazz == null) {
            throw new RuntimeException("Unknown Class");
        } else {
            FieldTypes type = (FieldTypes)valueTypes.get(clazz);
            if(type == null) {
                throw new RuntimeException("Unknown type:  " + clazz);
            } else {
                return (String)names.get(type);
            }
        }
    }

    public static Object changeType(Object value, FieldTypes type) {
        Object newObject = null;
        Class clazz = (Class)classes.get(type);
        if(clazz == null) {
            throw new RuntimeException("Unknown type:  " + type);
        } else if(value == null) {
            return null;
        } else {
            if(value instanceof String) {
                String bool = (String)value;
                switch(type.ordinal()) {
                    case 1:
                    case 2:
                    case 3:
                        bool = bool.trim();
                        newObject = bool.isEmpty()?null:Short.valueOf(Short.parseShort(bool));
                        break;
                    case 4:
                    case 5:
                        bool = bool.trim();
                        newObject = bool.isEmpty()?null:Long.valueOf(Long.parseLong(bool));
                        break;
                    case 6:
                    case 7:
                        bool = bool.trim();
                        newObject = bool.isEmpty()?null:Integer.valueOf(Integer.parseInt(bool));
                        break;
                    case 8:
                    case 9:
                    case 10:
                        newObject = value.toString();
                        break;
                    case 11:
                    case 12:
                        bool = bool.trim();
                        newObject = bool.isEmpty()?null:Byte.valueOf(Byte.parseByte(bool));
                        break;
                    case 13:
                        bool = bool.trim();
                        newObject = bool.isEmpty()?null:Boolean.valueOf(bool.equalsIgnoreCase("true"));
                        break;
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                }
            } else if(value instanceof Integer) {
                Integer bool1 = (Integer)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = Short.valueOf(bool1.shortValue());
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = Long.valueOf(bool1.longValue());
                        break;
                    case 7:
                        newObject = value;
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = Byte.valueOf(bool1.byteValue());
                        break;
                    case 13:
                        newObject = Boolean.valueOf(bool1.intValue() != 0);
                }
            } else if(value instanceof Double) {
                Double bool2 = (Double)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = Short.valueOf(bool2.shortValue());
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = Long.valueOf(bool2.longValue());
                        break;
                    case 7:
                        newObject = Integer.valueOf(bool2.intValue());
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = Byte.valueOf(bool2.byteValue());
                        break;
                    case 13:
                        newObject = Boolean.valueOf(bool2.doubleValue() != 0.0D);
                }
            } else if(value instanceof Float) {
                Float bool3 = (Float)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = Short.valueOf(bool3.shortValue());
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = Long.valueOf(bool3.longValue());
                        break;
                    case 7:
                        newObject = Integer.valueOf(bool3.intValue());
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = Byte.valueOf(bool3.byteValue());
                        break;
                    case 13:
                        newObject = Boolean.valueOf(bool3.floatValue() != 0.0F);
                }
            } else if(value instanceof Byte) {
                Byte bool4 = (Byte)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = Short.valueOf(bool4.shortValue());
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = Long.valueOf(bool4.longValue());
                        break;
                    case 7:
                        newObject = Integer.valueOf(bool4.intValue());
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = value;
                        break;
                    case 13:
                        newObject = Boolean.valueOf(bool4.byteValue() != 0);
                }
            } else if(value instanceof Short) {
                Short bool5 = (Short)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = value;
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = Long.valueOf(bool5.longValue());
                        break;
                    case 7:
                        newObject = Integer.valueOf(bool5.intValue());
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = Byte.valueOf(bool5.byteValue());
                        break;
                    case 13:
                        newObject = Boolean.valueOf(bool5.shortValue() != 0);
                }
            } else if(value instanceof Long) {
                Long bool6 = (Long)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = Short.valueOf(bool6.shortValue());
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = value;
                        break;
                    case 7:
                        newObject = Integer.valueOf(bool6.intValue());
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = Byte.valueOf(bool6.byteValue());
                        break;
                    case 13:
                        newObject = Boolean.valueOf(bool6.longValue() != 0L);
                }
            } else if(value instanceof Boolean) {
                Boolean bool7 = (Boolean)value;
                switch(type.ordinal()) {
                    case 2:
                        newObject = Short.valueOf((short)(bool7.booleanValue()?1:0));
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 9:
                    case 10:
                    case 12:
                    default:
                        throw new InvalidTypeException("Type:  " + type.toString());
                    case 4:
                        newObject = Long.valueOf((long)(bool7.booleanValue()?1:0));
                        break;
                    case 7:
                        newObject = Integer.valueOf(bool7.booleanValue()?1:0);
                        break;
                    case 8:
                        newObject = value.toString();
                        break;
                    case 11:
                        newObject = Byte.valueOf((byte)(bool7.booleanValue()?1:0));
                        break;
                    case 13:
                        newObject = value;
                }
            }

            return newObject;
        }
    }

    static {
        map = new HashMap();
        map.put("short", SHORT);
        map.put("byte", BYTE);
        map.put("i1", I1);
        map.put("boolean", BOOLEAN);
        map.put("unsignedByte", UNSIGNED_BYTE);
        map.put("i2", I2);
        map.put("unsignedShort", UNSIGNED_SHORT);
        map.put("i4", I4);
        map.put("int", INT);
        map.put("long", LONG);
        map.put("unsignedLong", UNSIGNED_LONG);
        map.put("string", STRING);
        map.put("l4string", L4STRING);
        map.put("bin.base64", BIN_BASE64);
        names = new HashMap();
        names.put(SHORT, "short");
        names.put(BYTE, "byte");
        names.put(I1, "i1");
        names.put(BOOLEAN, "boolean");
        names.put(UNSIGNED_BYTE, "unsignedByte");
        names.put(I2, "i2");
        names.put(UNSIGNED_SHORT, "unsignedShort");
        names.put(I4, "i4");
        names.put(INT, "int");
        names.put(LONG, "long");
        names.put(UNSIGNED_LONG, "unsignedLong");
        names.put(STRING, "string");
        names.put(L4STRING, "l4string");
        names.put(BIN_BASE64, "bin.base64");
        classes = new HashMap();
        classes.put(SHORT, Short.class);
        classes.put(BYTE, Byte.class);
        classes.put(I1, Byte.class);
        classes.put(BOOLEAN, Boolean.class);
        classes.put(UNSIGNED_BYTE, Byte.class);
        classes.put(I2, Short.class);
        classes.put(UNSIGNED_SHORT, Short.class);
        classes.put(I4, Integer.class);
        classes.put(INT, Integer.class);
        classes.put(LONG, Long.class);
        classes.put(UNSIGNED_LONG, Long.class);
        classes.put(STRING, String.class);
        classes.put(L4STRING, String.class);
        classes.put(BIN_BASE64, byte[].class);
        valueTypes = new HashMap();
        valueTypes.put(Byte.class, I1);
        valueTypes.put(Boolean.class, BOOLEAN);
        valueTypes.put(Short.class, I2);
        valueTypes.put(Integer.class, I4);
        valueTypes.put(Long.class, LONG);
        valueTypes.put(String.class, STRING);
        valueTypes.put(byte[].class, BIN_BASE64);
    }
}
