package com.rebirth.qarobot.scraping.utils;

import java.io.File;
import java.io.FileFilter;

public class AssetsFilterFile implements FileFilter {
    @Override
    public boolean accept(File asset) {
        String name = asset.getName();
        return asset.isFile() && (name.endsWith("png") || name.endsWith("txt"));
    }
}
