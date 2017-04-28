# Gradle transformers

This project contains some transformers for merging files with the **shadow** gradle plugin.

## Configuration

Install the project locally
```
gradle install
```

Reference the project in your build.gradle script.
```gradle
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
   dependencies {
        classpath "net.gcolin.tenserver:transformer:1.0"
    }
}
```

## Merge web-fragment.xml

```gradle
shadowJar {
   transform(net.gcolin.transformers.WebFragmentTransformer.class)
}
```

## Merge CDI beans.xml

```gradle
shadowJar {
   transform(new net.gcolin.transformers.XmlMergeTransformer(
        {path -> null }, ['META-INF/beans.xml']))
}
```

## Merge text files

The first line is used for ordering.

File 1 (META-INF/resources/hello.txt)
```
# 1

content1
```

File 2 (META-INF/resources/hello.txt)
```
# 2

content2
```

File merged (META-INF/resources/hello.txt)
```
# 1

content1
# 2

content2
```

```gradle
shadowJar {
    transform(new net.gcolin.transformers.OrderedTransformer([
        'META-INF/resources/*.txt'
    ]))
}
```

## Custom merge of xml files

File 1 (cacheconfig.xml)
```
<caches>
	<cache>
		<name>searchutil</name>
		<maxSizeMemory>50</maxSizeMemory>
		<expiryCreate>300000</expiryCreate>
		<expiryAccess>300000</expiryAccess>
		<expiryUpdate>300000</expiryUpdate>
		<statistics>false</statistics>
		<management>true</management>
	</cache>
<caches>
```

File 2 (cacheconfig.xml)
```
<caches>
	<cache>
		<name>latest</name>
		<maxSizeMemory>150</maxSizeMemory>
		<expiryCreate>600000</expiryCreate>
		<expiryAccess>600000</expiryAccess>
		<expiryUpdate>600000</expiryUpdate>
		<statistics>false</statistics>
		<management>true</management>
	</cache>
<caches>
```

File merged (cacheconfig.xml)
```
<caches>
	<cache>
		<name>searchutil</name>
		<maxSizeMemory>50</maxSizeMemory>
		<expiryCreate>300000</expiryCreate>
		<expiryAccess>300000</expiryAccess>
		<expiryUpdate>300000</expiryUpdate>
		<statistics>false</statistics>
		<management>true</management>
	</cache>
        <cache>
		<name>latest</name>
		<maxSizeMemory>150</maxSizeMemory>
		<expiryCreate>600000</expiryCreate>
		<expiryAccess>600000</expiryAccess>
		<expiryUpdate>600000</expiryUpdate>
		<statistics>false</statistics>
		<management>true</management>
	</cache>
<caches>
```

```gradle
shadowJar {
    transform(new net.gcolin.transformers.XmlMergeTransformer(
        {path -> "/caches/cache" }, ['cacheconfig.xml']))
}
```

