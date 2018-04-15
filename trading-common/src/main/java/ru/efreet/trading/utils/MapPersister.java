package ru.efreet.trading.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;

import java.util.Map;

public class MapPersister extends StringType {

    private static final MapPersister singleTon = new MapPersister();

    private static final Gson gson = new GsonBuilder().create();
    private static final TypeToken<Map<String, String>> tt = new TypeToken<Map<String, String>>() {
    };

    private MapPersister() {
        super(SqlType.STRING, new Class<?>[]{Map.class});
    }

    public static MapPersister getSingleton() {
        return singleTon;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        return gson.toJson(javaObject, tt.getType());

    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        return gson.fromJson((String) sqlArg, tt.getType());
    }
}