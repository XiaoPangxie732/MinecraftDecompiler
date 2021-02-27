/*
 * MinecraftDecompiler. A tool/library to deobfuscate and decompile Minecraft.
 * Copyright (C) 2019-2021  MaxPixelStudios
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

package cn.maxpixel.mcdecompiler.reader;

import cn.maxpixel.mcdecompiler.mapping.ClassMapping;
import cn.maxpixel.mcdecompiler.mapping.TinyClassMapping;
import cn.maxpixel.mcdecompiler.mapping.tiny.Namespaced;
import cn.maxpixel.mcdecompiler.mapping.tiny.TinyFieldMapping;
import cn.maxpixel.mcdecompiler.mapping.tiny.TinyMethodMapping;
import cn.maxpixel.mcdecompiler.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TinyMappingReader extends AbstractMappingReader {
    public TinyMappingReader(BufferedReader reader) {
        super(reader);
    }
    public TinyMappingReader(Reader rd) {
        super(rd);
    }
    public TinyMappingReader(InputStream is) {
        super(is);
    }
    public TinyMappingReader(String path) throws FileNotFoundException, NullPointerException {
        super(path);
    }
    private final V1TinyMappingProcessor V1_PROCESSOR = new V1TinyMappingProcessor();
    private final V2TinyMappingProcessor V2_PROCESSOR = new V2TinyMappingProcessor();
    @Override
    protected AbstractNonPackageMappingProcessor getProcessor() {
        try {
            reader.mark(50);
            String s = reader.readLine();
            reader.reset();
            if(s.startsWith("tiny\t2\t0\t")) return V2_PROCESSOR;
            else if(s.startsWith("v1\t")) return V1_PROCESSOR;
        } catch(IOException e) {
            LogManager.getLogger().catching(e);
        }
        return V2_PROCESSOR;
    }

    private static class V1TinyMappingProcessor extends AbstractNonPackageMappingProcessor {
        private String[] namespaces;
        @Override
        ObjectList<TinyClassMapping> process(Stream<String> lines) {
            ObjectArrayList<String> lns = lines.collect(Collectors.toCollection(ObjectArrayList::new));
            Object2ObjectOpenHashMap<String, TinyClassMapping> mappings = new Object2ObjectOpenHashMap<>(); // k: unmapped name
            namespaces = lns.remove(0).substring(3).split("\t");
            lns.forEach(s -> {
                if(s.startsWith("CLASS")) {
                    TinyClassMapping classMapping = processClass(s);
                    mappings.merge(classMapping.getUnmappedName(), classMapping, (o, n) -> {
                        n.addField(o.getFieldMap().values());
                        n.addMethod(o.getMethods());
                        return n;
                    });
                } else if(s.startsWith("FIELD")) {
                    TinyFieldMapping fieldMapping = processField(s);
                    TinyClassMapping cm = mappings.computeIfAbsent(fieldMapping.getOwner().getUnmappedName(), TinyClassMapping::new);
                    cm.addField(fieldMapping.setOwner(cm));
                } else if(s.startsWith("METHOD")) {
                    TinyMethodMapping methodMapping = processMethod(s);
                    TinyClassMapping cm = mappings.computeIfAbsent(methodMapping.getOwner().getUnmappedName(), TinyClassMapping::new);
                    cm.addMethod(methodMapping.setOwner(cm));
                } else throw new IllegalArgumentException("Is this a Tiny v1 mapping file?");
            });
            return null;
        }
        @Override
        protected TinyClassMapping processClass(String line) {
            String[] split = line.substring(6).split("\t");
            return new TinyClassMapping(Utils.mapArray(split, (i, s) -> new Namespaced(namespaces[i], s)));
        }
        @Override
        protected TinyMethodMapping processMethod(String line) {
            String[] split = line.split("\t");
            String[] names = new String[namespaces.length];
            System.arraycopy(split, 3, names, 0, namespaces.length);
            return new TinyMethodMapping(split[2], Utils.mapArray(names, (i, s) -> new Namespaced(namespaces[i], s)))
                    .setOwner(new TinyClassMapping(split[1]));
        }
        @Override
        protected TinyFieldMapping processField(String line) {
            String[] split = line.split("\t");
            String[] names = new String[namespaces.length];
            System.arraycopy(split, 3, names, 0, namespaces.length);
            return new TinyFieldMapping(split[2], Utils.mapArray(names, (i, s) -> new Namespaced(namespaces[i], s)))
                    .setOwner(new TinyClassMapping(split[1]));
        }
    }
    public static class V2TinyMappingProcessor extends AbstractNonPackageMappingProcessor {
        @Override
        public ObjectList<ClassMapping> process(Stream<String> lines) {
            return null;
        }
        @Override
        protected TinyClassMapping processClass(String line) {
            return null;
        }
        @Override
        protected TinyMethodMapping processMethod(String line) {
            return null;
        }
        @Override
        protected TinyFieldMapping processField(String line) {
            return null;
        }
    }
}