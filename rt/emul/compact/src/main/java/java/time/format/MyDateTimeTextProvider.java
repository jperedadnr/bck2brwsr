/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package java.time.format;

import java.time.format.SimpleDateTimeTextProvider.LocaleStore;
import java.time.temporal.TemporalField;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author johan
 */
public class MyDateTimeTextProvider extends DateTimeTextProvider {

    private LocaleStore store;

    public MyDateTimeTextProvider(LocaleStore store) {
        this.store = store;
    }

    @Override
    public String getText(TemporalField field, long value, TextStyle style, Locale locale) {
        return store.getText(value, style);
    }

    @Override
    public Iterator<Map.Entry<String, Long>> getTextIterator(TemporalField field, TextStyle style, Locale locale) {
        return store.getTextIterator(style);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return new Locale[0];
//                throw new UnsupportedOperationException();
    }


}
