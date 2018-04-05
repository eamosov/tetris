package ru.efreet.trading.utils;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.DateTimeType;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DateTimePersister extends DateTimeType {

    private static final DateTimePersister singleTon = new DateTimePersister();

    private DateTimePersister() {
        super(SqlType.DATE, new Class<?>[]{ZonedDateTime.class});
    }

    public static DateTimePersister getSingleton() {
        return singleTon;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        ZonedDateTime dateTime = (ZonedDateTime) javaObject;
        if (dateTime == null) {
            return null;
        } else {
            return dateTime.toEpochSecond() * 1000L;
        }
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli((Long) sqlArg), ZoneId.of("GMT"));
    }
}