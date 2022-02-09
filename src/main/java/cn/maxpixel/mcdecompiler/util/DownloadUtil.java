/*
 * MinecraftDecompiler. A tool/library to deobfuscate and decompile Minecraft.
 * Copyright (C) 2019-2022  MaxPixelStudios
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.maxpixel.mcdecompiler.util;

import cn.maxpixel.mcdecompiler.Info;
import cn.maxpixel.mcdecompiler.Properties;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cn.maxpixel.mcdecompiler.MinecraftDecompiler.HTTP_CLIENT;
import static java.nio.file.StandardOpenOption.*;

public class DownloadUtil {
    private static final Logger LOGGER = Logging.getLogger("Download Utility");

    public static BufferedReader downloadMapping(String version, Info.SideType type) {
        JsonObject versionDownloads = VersionManifest.get(version).get("downloads").getAsJsonObject();
        if(!versionDownloads.has(type + "_mappings"))
            throw new IllegalArgumentException("Version \"" + version + "\" doesn't contain Proguard mappings. Please use 1.14.4 or above");
        Path p = Properties.getDownloadedProguardMappingPath(version, type);
        if(Files.notExists(p)) {
            try {
                LOGGER.info("Downloading mapping...");
                HttpRequest request = HttpRequest
                        .newBuilder(URI.create(versionDownloads.get(type + "_mappings").getAsJsonObject().get("url").getAsString()))
                        .build();
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(FileUtil.ensureFileExist(p), WRITE, TRUNCATE_EXISTING));
            } catch(IOException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Error downloading Proguard mapping file", e);
                throw Utils.wrapInRuntime(e);
            }
        }
        try {
            return Files.newBufferedReader(p);
        } catch(IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating BufferedReader for Proguard mapping file", e);
            throw Utils.wrapInRuntime(e);
        }
    }

    /**
     * Download the remote resource if the local file is invalid
     * @param localPath The local file
     * @param remoteResource The URI of the resource
     * @param remoteHash The hash file of the resource
     * @return The input stream of the local file
     */
    public static InputStream getRemoteResource(Path localPath, URI remoteResource, URI remoteHash) throws IOException {
        try {
            if(!FileUtil.verify(Objects.requireNonNull(localPath), HTTP_CLIENT.send(HttpRequest.newBuilder(remoteHash).build(),
                    HttpResponse.BodyHandlers.ofString()).body(), -1L)) {
                FileUtil.deleteIfExists(localPath);
                LOGGER.fine("Downloading the resource");
                FileUtil.ensureFileExist(localPath);
                HTTP_CLIENT.send(HttpRequest.newBuilder(remoteResource).build(),
                        HttpResponse.BodyHandlers.ofFile(localPath, WRITE, TRUNCATE_EXISTING));
            }
        } catch(InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Download process interrupted", e);
            throw Utils.wrapInRuntime(e);
        }
        return Files.newInputStream(localPath, READ);
    }
}