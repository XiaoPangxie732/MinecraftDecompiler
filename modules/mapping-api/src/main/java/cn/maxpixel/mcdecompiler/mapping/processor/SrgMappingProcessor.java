package cn.maxpixel.mcdecompiler.mapping.processor;

import cn.maxpixel.mcdecompiler.mapping.PairedMapping;
import cn.maxpixel.mcdecompiler.mapping.collection.ClassMapping;
import cn.maxpixel.mcdecompiler.mapping.collection.ClassifiedMapping;
import cn.maxpixel.mcdecompiler.mapping.format.MappingFormat;
import cn.maxpixel.mcdecompiler.mapping.format.MappingFormats;
import cn.maxpixel.mcdecompiler.mapping.util.MappingUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.function.Function;

public enum SrgMappingProcessor implements MappingProcessor.Classified<PairedMapping> {
    INSTANCE;

    private static final Function<String, Function<String, ClassMapping<PairedMapping>>> MAPPING_FUNC = s ->
            k -> new ClassMapping<>(new PairedMapping(k, getClassName(s)));

    @Override
    public MappingFormat<PairedMapping, ClassifiedMapping<PairedMapping>> getFormat() {
        return MappingFormats.SRG;
    }

    @Override
    public ClassifiedMapping<PairedMapping> process(ObjectList<String> content) {
        ClassifiedMapping<PairedMapping> mappings = new ClassifiedMapping<>();
        Object2ObjectOpenHashMap<String, ClassMapping<PairedMapping>> classes = new Object2ObjectOpenHashMap<>(); // k: unmapped name
        content.parallelStream().forEach(s -> {
            String[] strings = MappingUtil.split(s, ' ');
            switch (strings[0]) {
                case "CL:" -> {
                    ClassMapping<PairedMapping> classMapping = new ClassMapping<>(new PairedMapping(strings[1], strings[2]));
                    synchronized (classes) {
                        classes.putIfAbsent(strings[1], classMapping);
                    }
                }
                case "FD:" -> {
                    PairedMapping fieldMapping = MappingUtil.Paired.o(getName(strings[1]), getName(strings[2]));
                    String unmClassName = getClassName(strings[1]);
                    synchronized (classes) {
                        classes.computeIfAbsent(unmClassName, MAPPING_FUNC.apply(strings[2]))
                                .addField(fieldMapping);
                    }
                }
                case "MD:" -> {
                    PairedMapping methodMapping = MappingUtil.Paired.d2o(getName(strings[1]), getName(strings[3]), strings[2], strings[4]);
                    String unmClassName = getClassName(strings[1]);
                    synchronized (classes) {
                        classes.computeIfAbsent(unmClassName, MAPPING_FUNC.apply(strings[3]))
                                .addMethod(methodMapping);
                    }
                }
                case "PK:" -> {
                    synchronized (mappings.packages) {
                        mappings.packages.add(new PairedMapping(strings[1], strings[2]));
                    }
                }
                default -> throw new IllegalArgumentException("Is this a SRG mapping file?");
            }
        });
        mappings.classes.addAll(classes.values());
        return mappings;
    }

    private static String getClassName(String s) {
        return s.substring(0, s.lastIndexOf('/'));
    }

    private static String getName(String s) {
        return s.substring(s.lastIndexOf('/') + 1);
    }
}