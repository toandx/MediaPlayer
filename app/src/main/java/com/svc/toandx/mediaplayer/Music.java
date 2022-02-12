package com.svc.toandx.mediaplayer;
import java.io.Serializable;

public class Music implements Serializable {
    public String title, path;
    public Music(String mTitle,String mPath)
    {
        this.title=mTitle;
        this.path=mPath;
    }
}
