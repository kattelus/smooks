/*
        Milyn - Copyright (C) 2006 - 2010

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Lesser General Public
        License (version 2.1) as published by the Free Software
        Foundation.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

        See the GNU Lesser General Public License for more details:
        http://www.gnu.org/licenses/lgpl.txt
*/
package org.milyn.javabean.decoders;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

import org.milyn.container.ExecutionContext;
import org.milyn.delivery.Filter;
import org.milyn.edisax.model.internal.Delimiters;
import org.milyn.javabean.DataDecodeException;
import org.milyn.javabean.DecodeType;

/**
 * {@link BigDecimal} Decoder, which is EDI delimiters aware for parsing decimal.
 *
 * @author <a href="mailto:sinfomicien@gmail.com">sinfomicien@gmail.com</a>
 * @author <a href="mailto:michael@krueske.net">michael@krueske.net</a> (patched to ensure that always a {@link BigDecimal} value is decoded)
 */
@DecodeType(BigDecimal.class)
public class DABigDecimalDecoder extends BigDecimalDecoder {

    @Override
    public Object decode(String data) throws DataDecodeException {
        DecimalFormat decimalFormat = getDecimalFormat();
        setDecimalPointFormat(decimalFormat, getContextDelimiters());

        final Number number;
        try {
            number = decimalFormat.parse(data.trim());
        } catch (final ParseException e) {
            throw new DataDecodeException("Failed to decode BigDecimal value '" + data
                    + "' using NumberFormat instance " + decimalFormat + ".", e);
        }

        return number;
    }

    @Override
    public String encode(Object object) throws DataDecodeException {
        DecimalFormat decimalFormat = getDecimalFormat();
        return decimalFormat.format(object);
    }

    //Thread safe function to encode with delimiters awareness
    public String encode(Object object, Delimiters interchangeDelimiters) throws DataDecodeException {
        DecimalFormat decimalFormat = getDecimalFormat();
        setDecimalPointFormat(decimalFormat, interchangeDelimiters);
        // Let's not limit fraction digits but use digits from actual BigDecimal scale
        decimalFormat.setMaximumFractionDigits(Math.max(
                ((BigDecimal) object).stripTrailingZeros().scale(),
                decimalFormat.getMaximumFractionDigits()));
        decimalFormat.setRoundingMode(RoundingMode.UNNECESSARY);

        return decimalFormat.format(object);
    }

    private synchronized DecimalFormat getDecimalFormat() {
        //Check to see if we can use the parent default format
        NumberFormat parentNumberFormat = getNumberFormat();

        if (parentNumberFormat != null && parentNumberFormat instanceof DecimalFormat) {
            // Clone because we potentially need to modify the decimal point...
            return (DecimalFormat) parentNumberFormat.clone();
        } else {
            return new DecimalFormat();
        }
    }

    private synchronized void setDecimalPointFormat(DecimalFormat decimalFormat, Delimiters interchangeDelimiters) {
        DecimalFormatSymbols dfs = decimalFormat.getDecimalFormatSymbols();

        decimalFormat.applyPattern("#0.0#");
        if (interchangeDelimiters != null) {
            dfs.setDecimalSeparator(interchangeDelimiters.getDecimalSeparator().charAt(0));
        }
        decimalFormat.setDecimalFormatSymbols(dfs);
        decimalFormat.setParseBigDecimal(true);
    }

    protected Delimiters getContextDelimiters() {
        ExecutionContext ec = Filter.getCurrentExecutionContext();
        Delimiters delimiters = null;

        if (ec != null) {
            delimiters = ec.getBeanContext().getBean(Delimiters.class);
        }

        return delimiters;
    }
}
