package com.aware.plugin.sensortag;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import static com.aware.plugin.sensortag.SensorTagGatt.UUID_ACC_CONF;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_HUM_CONF;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_HUM_DATA;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_HUM_SERV;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_IRT_DATA;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_IRT_SERV;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_MOV_CONF;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_MOV_DATA;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_MOV_SERV;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_OPT_CONF;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_OPT_DATA;
import static com.aware.plugin.sensortag.SensorTagGatt.UUID_OPT_SERV;
import static java.lang.Math.pow;

/**
 * Created by aayushchadha on 18/08/17.
 */

/**
 * Methods to convert byte arrays into readings that can be dumped into database
 * Taken from jordanleex13/SensorTag and modified for this application.
 * Reference link - https://github.com/jordanleex13/SensorTag
 */
public enum SensorConversion {
    /*
    * The IR temperature sensor provides two readings
    * 1) Object Temperature
    * 2) Ambient Temperature
    *
    * Both of these require conversion and the former is dependent on the latter
    * These are supposedly stored as (ObjLSB, ObjMSB, AmbLSB, AmbMSB) (4 bytes) which means
    * some byte shifting is required.
    */

    IR_TEMPERATURE(UUID_IRT_SERV, UUID_IRT_DATA, UUID_ACC_CONF) {
        @Override
        public Measurement convert(final byte[] value) {

            double ambient = extractAmbientTemperature(value);
            double target = extractTargetTemperature(value, ambient);

            double targetNewSensor = extractTargetTemperatureTMP007(value);

            return new Measurement(ambient, target, targetNewSensor);

        }

        private double extractAmbientTemperature(byte[] v) {
            int offset = 2;
            return shortUnsignedAtOffset(v, offset) / 128.0;
        }

        private double extractTargetTemperature(byte[] v, double ambient) {
            Integer twoByteValue = shortSignedAtOffset(v, 0);

            double Vobj2 = twoByteValue.doubleValue();

            Vobj2 *= 0.00000015625;

            double Tdie = ambient + 273.15;

            double S0 = 5.593E-14; // Calibration factor
            double a1 = 1.75E-3;
            double a2 = -1.678E-5;
            double b0 = -2.94E-5;
            double b1 = -5.7E-7;
            double b2 = 4.63E-9;
            double c2 = 13.4;
            double Tref = 298.15;
            double S = S0 * (1 + a1 * (Tdie - Tref) + a2 * pow((Tdie - Tref), 2));
            double Vos = b0 + b1 * (Tdie - Tref) + b2 * pow((Tdie - Tref), 2);
            double fObj = (Vobj2 - Vos) + c2 * pow((Vobj2 - Vos), 2);
            double tObj = pow(pow(Tdie, 4) + (fObj / S), .25);

            return tObj - 273.15;


        }

        private double extractTargetTemperatureTMP007(byte[] v) {
            int offset = 0;
            return shortUnsignedAtOffset(v, offset) / 128.0;
        }
    },

    MOVEMENT_ACC(UUID_MOV_SERV, UUID_MOV_DATA, UUID_MOV_CONF, (byte) 3) {
        @Override
        public Measurement convert(final byte[] value) {

            // Range 8G
            final float SCALE = (float) 4096.0;

            int x = (value[7] << 8) + value[6];
            int y = (value[9] << 8) + value[8];
            int z = (value[11] << 8) + value[10];
            return new Measurement(((x / SCALE) * -1), y / SCALE, ((z / SCALE) * -1));

        }

    },

    MOVEMENT_GYRO(UUID_MOV_SERV, UUID_MOV_DATA, UUID_MOV_CONF, (byte) 3) {
        @Override
        public Measurement convert(final byte[] value) {

            final float SCALE = (float) 128.0;

            int x = (value[1] << 8) + value[0];
            int y = (value[3] << 8) + value[2];
            int z = (value[5] << 8) + value[4];
            return new Measurement(x / SCALE, y / SCALE, z / SCALE);
        }

    },

    MOVEMENT_MAG(UUID_MOV_SERV, UUID_MOV_DATA, UUID_MOV_CONF, (byte) 3) {
        @Override
        public Measurement convert(final byte[] value) {
            final float SCALE = (float) (32768 / 4912);
            if (value.length >= 18) {
                int x = (value[13] << 8) + value[12];
                int y = (value[15] << 8) + value[14];
                int z = (value[17] << 8) + value[16];
                return new Measurement(x / SCALE, y / SCALE, z / SCALE);
            } else {
                return new Measurement(0, 0, 0);
            }
        }
    },

    HUMIDITY(UUID_HUM_SERV, UUID_HUM_DATA, UUID_HUM_CONF) {
        @Override
        public Measurement convert(final byte[] value) {
            int a = shortUnsignedAtOffset(value, 2);
            // bits [1..0] are status bits and need to be cleared according
            // to the user guide, but the iOS code doesn't bother. It should
            // have minimal impact.
            a = a - (a % 4);

            return new Measurement((-6f) + 125f * (a / 65535f), 0, 0);
        }
    },

    HUMIDITY2(UUID_HUM_SERV, UUID_HUM_DATA, UUID_HUM_CONF) {
        @Override
        public Measurement convert(final byte[] value) {
            int a = shortUnsignedAtOffset(value, 2);

            return new Measurement(100f * (a / 65535f), 0, 0);
        }
    },

    LUXOMETER(UUID_OPT_SERV, UUID_OPT_DATA, UUID_OPT_CONF) {
        @Override
        public Measurement convert(final byte[] value) {
            int mantissa;
            int exponent;
            Integer sfloat = shortUnsignedAtOffset(value, 0);

            mantissa = sfloat & 0x0FFF;
            exponent = (sfloat >> 12) & 0xFF;

            double output;
            double magnitude = pow(2.0f, exponent);
            output = (mantissa * magnitude);

            return new Measurement(output / 100.0f, 0, 0);
        }
    },

    BAROMETER(SensorTagGatt.UUID_BAR_SERV, SensorTagGatt.UUID_BAR_DATA, SensorTagGatt.UUID_BAR_CONF) {
        @Override
        public Measurement convert(final byte[] value) {

            //            if (DeviceActivity.getInstance().isSensorTag2())

            if (value.length > 4) {
                Integer val = twentyFourBitUnsignedAtOffset(value, 2);
                return new Measurement((double) val / 100.0, 0, 0);
            } else {
                int mantissa;
                int exponent;
                Integer sfloat = shortUnsignedAtOffset(value, 2);

                mantissa = sfloat & 0x0FFF;
                exponent = (sfloat >> 12) & 0xFF;

                double output;
                double magnitude = pow(2.0f, exponent);
                output = (mantissa * magnitude);
                return new Measurement(output / 100.0f, 0, 0);

            }
        }
    };

    private final UUID service, data, config;
    private byte enableCode; // See getEnableSensorCode for explanation.
    public static final byte DISABLE_SENSOR_CODE = 0;
    public static final byte ENABLE_SENSOR_CODE = 1;
    public static final byte CALIBRATE_SENSOR_CODE = 2;

    /**
     * Standard constructor for all sensors apart from movement, since movement sensor
     * requires specific enable bits.
     *
     * @param uuidServ
     * @param uuidData
     * @param uuidConf
     */
    SensorConversion(UUID uuidServ, UUID uuidData, UUID uuidConf) {
        this.service = uuidServ;
        this.data = uuidData;
        this.config = uuidConf;
        this.enableCode = ENABLE_SENSOR_CODE;
    }

    SensorConversion(UUID uuidMovServ, UUID uuidMovData, UUID uuidMovConf, byte b) {
        this.service = uuidMovServ;
        this.data = uuidMovData;
        this.config = uuidMovConf;
        this.enableCode = b;
    }

    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's complement values as LSB MSB, which cannot be directly parsed
     * as getIntValue(FORMAT_SINT16, offset) because the bytes are stored as little-endian.
     * <p>
     * This function extracts these 16 bit two's complement values.
     */
    private static Integer shortSignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1]; // // Interpret MSB as signed
        return (upperByte << 8) + lowerByte;
    }

    private static Integer shortUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer upperByte = (int) c[offset + 1] & 0xFF;
        return (upperByte << 8) + lowerByte;
    }

    private static Integer twentyFourBitUnsignedAtOffset(byte[] c, int offset) {
        Integer lowerByte = (int) c[offset] & 0xFF;
        Integer mediumByte = (int) c[offset + 1] & 0xFF;
        Integer upperByte = (int) c[offset + 2] & 0xFF;
        return (upperByte << 16) + (mediumByte << 8) + lowerByte;
    }

    public void onCharacteristicChanged(BluetoothGattCharacteristic c) {
        throw new UnsupportedOperationException("Error: the individual enum classes are supposed to override this method.");
    }

    public Measurement convert(byte[] value) {
        throw new UnsupportedOperationException("Error: the individual enum classes are supposed to override this method.");
    }

    /**
     * @return the code which, when written to the configuration characteristic, turns on the sensor.
     */
    public byte getEnableSensorCode() {
        return enableCode;
    }

    public UUID getService() {
        return service;
    }

    public UUID getData() {
        return data;
    }

    public UUID getConfig() {
        return config;
    }

    public static SensorConversion getFromDataUuid(UUID uuid) {
        for (SensorConversion s : SensorConversion.values()) {
            if (s.getData().equals(uuid)) {
                return s;
            }
        }
        throw new RuntimeException("unable to find UUID.");
    }

}

