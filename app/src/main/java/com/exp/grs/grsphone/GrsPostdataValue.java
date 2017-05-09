package com.exp.grs.grsphone;

/**
 * Created by freem on 2016-12-20.
 */

public class GrsPostdataValue {
    String urlTo;
    String value;
    byte[] data;
    GrsPostdataValue( String _urlTo,String _value,byte[] _data)
    {
        urlTo=_urlTo;
        value=_value;
        data = _data;
    }


}
