package ru.efreet.trading.utils;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.DateTimeType;
import ru.efreet.trading.exchange.Instrument;

public class InstrumentPersister extends DateTimeType {

    private static final InstrumentPersister singleTon = new InstrumentPersister();

    private InstrumentPersister() {
        super(SqlType.STRING, new Class<?>[]{Instrument.class});
    }

    public static InstrumentPersister getSingleton() {
        return singleTon;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        Instrument instrument = (Instrument) javaObject;
        if (instrument == null) {
            return null;
        } else {
            return instrument.toString();
        }
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        String args[] = ((String) sqlArg).split("_");
        return new Instrument(args[0], args[1]);
    }
}