package top.fifthlight.mergetools;

import top.fifthlight.mergetools.processor.ExpectData;

import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

sealed interface MergeEntry {
    record ExpectManifest(String interfaceFullQualifiedName, ExpectData data) implements MergeEntry {
    }

    record JarItem(JarFile jarFile, JarEntry entry) implements MergeEntry {
    }

    record ResourceFile(Path path) implements MergeEntry {
    }
}