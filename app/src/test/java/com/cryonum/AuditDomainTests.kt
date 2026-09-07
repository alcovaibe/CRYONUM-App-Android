package com.cryonum

import com.cryonum.math.*
import com.cryonum.pdf.PdfRenderBudget
import org.junit.Assert.*
import org.junit.Test

class AuditDomainTests {
    @Test fun bijectionValidationAndTopRowOrder() {
        assertEquals(listOf(2,1,3),PermutationInput.normalized("213","123"))
        assertEquals(1,PermutationUtils.countInversions(PermutationInput.normalized("213","123")))
        for(s in listOf("112","124","1230","12a","1 2 99999999999999999999")) assertThrows(IllegalArgumentException::class.java){PermutationInput.row(s)}
        assertEquals(10,PermutationInput.row("1 2 3 4 5 6 7 8 9 10").size)
    }
    @Test fun allSmallPermutationsAgainstCycleParity() {
        fun visit(xs: List<Int>, prefix: List<Int>) {
            if(xs.isNotEmpty()){xs.forEach {visit(xs-it,prefix+it)};return}
            var cycles=0; val seen=BooleanArray(prefix.size)
            for(i in prefix.indices) if(!seen[i]){cycles++; var j=i;while(!seen[j]){seen[j]=true;j=prefix[j]-1}}
            assertEquals((prefix.size-cycles)%2==0,PermutationUtils.calculateParity(PermutationUtils.countInversions(prefix)))
        }
        visit((1..7).toList(),emptyList())
    }
    @Test fun pdfAllocationBounds() {
        for((w,h) in listOf(595 to 842, 1 to Int.MAX_VALUE, Int.MAX_VALUE to 1, Int.MAX_VALUE to Int.MAX_VALUE)) {
            val (x,y)=PdfRenderBudget.dimensions(w,h); assertTrue(x>0&&y>0);assertTrue(x.toLong()*y<=PdfRenderBudget.MAX_PIXELS)
        }
        assertThrows(IllegalArgumentException::class.java){PdfRenderBudget.dimensions(0,10)}
    }
}
