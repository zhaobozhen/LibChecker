package com.absinthe.libchecker.utils.elf

import com.absinthe.libchecker.annotation.ET_NOT_ELF
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ELF (Executable and Linkable Format) file parser
 * Used to parse ELF file format, extract header information and program headers
 */
class ELFParser(inputStream: InputStream) {

    // ELF header information
    private var elfHeader = ElfHeader()
    
    // Program headers list
    private val programHeaders: MutableList<ProgramHeader> = mutableListOf()
    
    // Number of bytes read
    var readBytes: Long = 0
        private set
    
    init {
        parse(inputStream)
    }
    
    /**
     * Determines if the file is a valid ELF file
     * @return true if the file is a valid ELF file
     */
    fun isElf(): Boolean = elfHeader.isValidElf()
    
    /**
     * Gets the ELF file type
     * @return file type constant
     */
    fun getEType(): Int {
        return if (!isElf()) {
            ET_NOT_ELF
        } else {
            elfHeader.type.toInt()
        }
    }
    
    /**
     * Gets the ELF file class (32-bit or 64-bit)
     * @return file class constant
     */
    fun getEClass(): Int {
        return if (!isElf()) {
            ET_NOT_ELF
        } else {
            elfHeader.ident.elfClass.toInt()
        }
    }
    
    /**
     * Gets the minimum page size
     * Determined by analyzing LOAD type segments in program headers
     * @return minimum page size or -1 (if not available)
     */
    fun getMinPageSize(): Int {
        var minAlign: Long? = null
        programHeaders.forEach { header ->
            if (header.type == ProgramHeader.PT_LOAD) {
                minAlign = minAlign?.coerceAtMost(header.align) ?: header.align
            }
        }
        return minAlign?.toInt() ?: -1
    }
    
    /**
     * Parses the ELF file
     * @param inputStream input stream
     */
    private fun parse(inputStream: InputStream) {
        try {
            // 1. Read and parse ELF identification
            parseElfIdent(inputStream)
            
            if (!isElf()) {
                return
            }
            
            // 2. Determine byte order and header size
            val headerSize = determineHeaderSize()
            val byteOrder = determineByteOrder()
            
            // 3. Read and parse ELF header
            parseElfHeader(inputStream, headerSize, byteOrder)
            
            // 4. Read and parse program headers
            parseProgramHeaders(inputStream, byteOrder)
        } catch (e: Exception) {
            // Keep default state if parsing fails
        }
    }
    
    /**
     * Parses ELF identification information
     */
    private fun parseElfIdent(inputStream: InputStream) {
        val identBuffer = ByteArray(EI_NIDENT)
        readFully(inputStream, identBuffer, EI_NIDENT)
        readBytes += EI_NIDENT
        elfHeader.ident = EIdent(identBuffer)
    }
    
    /**
     * Determines the ELF header size
     */
    private fun determineHeaderSize(): Int = when (getEClass()) {
        EIdent.ELFCLASS32 -> 36 // 32-bit ELF header size
        EIdent.ELFCLASS64 -> 48 // 64-bit ELF header size
        else -> 0
    }
    
    /**
     * Determines the byte order
     */
    private fun determineByteOrder(): ByteOrder =
        if (elfHeader.ident.dataEncoding.toInt() == EIdent.ELFDATA2MSB) {
            ByteOrder.BIG_ENDIAN
        } else {
            ByteOrder.LITTLE_ENDIAN
        }
    
    /**
     * Parses the ELF header information
     */
    private fun parseElfHeader(inputStream: InputStream, headerSize: Int, byteOrder: ByteOrder) {
        if (headerSize == 0) return
        
        val buffer = ByteBuffer.allocate(headerSize).order(byteOrder)
        readFully(inputStream, buffer.array(), headerSize)
        readBytes += headerSize
        
        when (getEClass()) {
            EIdent.ELFCLASS32 -> parseElf32Header(buffer)
            EIdent.ELFCLASS64 -> parseElf64Header(buffer)
        }
    }
    
    /**
     * Parses 32-bit ELF header
     */
    private fun parseElf32Header(buffer: ByteBuffer) {
        with(elfHeader) {
            type = buffer.short
            machine = buffer.short
            version = buffer.int
            entry = buffer.int.toLong() and 0xFFFFFFFFL
            programHeaderOffset = buffer.int.toLong() and 0xFFFFFFFFL
            sectionHeaderOffset = buffer.int.toLong() and 0xFFFFFFFFL
            flags = buffer.int
            headerSize = buffer.short
            programHeaderEntrySize = buffer.short
            programHeaderNumber = buffer.short
            sectionHeaderEntrySize = buffer.short
            sectionHeaderNumber = buffer.short
            sectionHeaderStringIndex = buffer.short
        }
    }
    
    /**
     * Parses 64-bit ELF header
     */
    private fun parseElf64Header(buffer: ByteBuffer) {
        with(elfHeader) {
            type = buffer.short
            machine = buffer.short
            version = buffer.int
            entry = buffer.long
            programHeaderOffset = buffer.long
            sectionHeaderOffset = buffer.long
            flags = buffer.int
            headerSize = buffer.short
            programHeaderEntrySize = buffer.short
            programHeaderNumber = buffer.short
            sectionHeaderEntrySize = buffer.short
            sectionHeaderNumber = buffer.short
            sectionHeaderStringIndex = buffer.short
        }
    }
    
    /**
     * Parses program headers
     */
    private fun parseProgramHeaders(inputStream: InputStream, byteOrder: ByteOrder) {
        val phOffset = elfHeader.programHeaderOffset
        val phNum = elfHeader.programHeaderNumber
        val phEntSize = elfHeader.programHeaderEntrySize
        
        if (phOffset <= 0 || phNum <= 0) return
        
        for (i in 0 until phNum) {
            val phBuffer = ByteBuffer.allocate(phEntSize.toInt()).order(byteOrder)
            readFully(inputStream, phBuffer.array(), phEntSize.toInt())
            readBytes += phEntSize.toInt()
            
            val programHeader = if (getEClass() == EIdent.ELFCLASS32) {
                parseProgramHeader32(phBuffer)
            } else {
                parseProgramHeader64(phBuffer)
            }
            
            programHeaders.add(programHeader)
        }
    }
    
    /**
     * Parses 32-bit program header
     */
    private fun parseProgramHeader32(buffer: ByteBuffer): ProgramHeader {
        return ProgramHeader(
            type = buffer.int,
            offset = buffer.int.toLong(),
            vaddr = buffer.int.toLong(),
            paddr = buffer.int.toLong(),
            fileSize = buffer.int.toLong(),
            memSize = buffer.int.toLong(),
            flags = buffer.int,
            align = buffer.int.toLong()
        )
    }
    
    /**
     * Parses 64-bit program header
     */
    private fun parseProgramHeader64(buffer: ByteBuffer): ProgramHeader {
        return ProgramHeader(
            type = buffer.int,
            flags = buffer.int,
            offset = buffer.long,
            vaddr = buffer.long,
            paddr = buffer.long,
            fileSize = buffer.long,
            memSize = buffer.long,
            align = buffer.long
        )
    }
    
    /**
     * Fully reads specified length of data from the input stream
     */
    private fun readFully(inputStream: InputStream, buffer: ByteArray, length: Int) {
        var bytesRead = 0
        
        while (bytesRead < length) {
            val readCount = inputStream.read(buffer, bytesRead, length - bytesRead)
            if (readCount == -1) {
                throw EOFException("Unexpected end of stream while reading $length bytes")
            }
            bytesRead += readCount
        }
    }
    
    /**
     * ELF file header information
     */
    private data class ElfHeader(
        var ident: EIdent = EIdent(ByteArray(EI_NIDENT)),
        var type: Short = 0,
        var machine: Short = 0,
        var version: Int = 0,
        var entry: Long = 0,
        var programHeaderOffset: Long = 0,
        var sectionHeaderOffset: Long = 0,
        var flags: Int = 0,
        var headerSize: Short = 0,
        var programHeaderEntrySize: Short = 0,
        var programHeaderNumber: Short = 0,
        var sectionHeaderEntrySize: Short = 0,
        var sectionHeaderNumber: Short = 0,
        var sectionHeaderStringIndex: Short = 0
    ) {
        fun isValidElf(): Boolean {
            return ident.magic0 == 0x7F.toByte() &&
                   ident.magic1.toInt().toChar() == 'E' &&
                   ident.magic2.toInt().toChar() == 'L' &&
                   ident.magic3.toInt().toChar() == 'F'
        }
    }
    
    /**
     * ELF identification information
     */
    class EIdent(array: ByteArray) {
        val magic0 = array[0]  // Must be 0x7F
        val magic1 = array[1]  // Must be 'E'
        val magic2 = array[2]  // Must be 'L'
        val magic3 = array[3]  // Must be 'F'
        val elfClass = array[4]  // 1 = 32-bit, 2 = 64-bit
        val dataEncoding = array[5]  // 1 = little-endian, 2 = big-endian
        val version = array[6]  // ELF identification version
        val osAbi = array[7]  // Target OS ABI
        val abiVersion = array[8]  // ABI version
        val padding = array.sliceArray(9..14)  // Padding bytes
        val nident = array[15]  // Identification size
        
        companion object {
            const val ELFCLASSNONE = 0  // Invalid class
            const val ELFCLASS32 = 1    // 32-bit objects
            const val ELFCLASS64 = 2    // 64-bit objects
            const val ELFDATA2MSB = 2   // Big-endian encoding
        }
    }
    
    /**
     * Program header information
     */
    data class ProgramHeader(
        val type: Int,       // Segment type
        val offset: Long,    // Segment file offset
        val vaddr: Long,     // Segment virtual address
        val paddr: Long,     // Segment physical address
        val fileSize: Long,  // Segment size in file
        val memSize: Long,   // Segment size in memory
        val flags: Int,      // Segment flags
        val align: Long      // Segment alignment
    ) {
        companion object {
            const val PT_LOAD = 1  // Loadable segment
        }
    }
    
    companion object {
        private const val EI_NIDENT = 16  // ELF identification size
        
        // ELF32 constants
        private const val ELF32_ADDR = 4  // Address size
        private const val ELF32_HALF = 2  // Half word size
        private const val ELF32_OFF = 4   // Offset size
        private const val ELF32_WORD = 4  // Word size
        
        // ELF64 constants
        private const val ELF64_ADDR = 8  // Address size
        private const val ELF64_HALF = 2  // Half word size
        private const val ELF64_OFF = 8   // Offset size
        private const val ELF64_WORD = 4  // Word size
        private const val ELF64_XWORD = 8 // Extended word size
    }
}
