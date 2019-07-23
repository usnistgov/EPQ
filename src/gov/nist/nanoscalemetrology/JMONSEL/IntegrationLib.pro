/* -------------------------------------------------------------------
   File "Integration_Lib.pro"

   Definition of integration methods
   ------------------------------------------------------------------- */

Integration {
  { Name GradGrad ;
    Case { {Type Gauss ;
            Case { { GeoElement Triangle    ; NumberOfPoints  4 ; }
                   { GeoElement Quadrangle  ; NumberOfPoints  4 ; }
                   { GeoElement Tetrahedron ; NumberOfPoints  4 ; }
                   { GeoElement Hexahedron  ; NumberOfPoints  6 ; }
                   { GeoElement Prism       ; NumberOfPoints  9 ; } }
           }
         }
  }
  { Name CurlCurl ;
    Case { {Type Gauss ;
            Case { { GeoElement Triangle    ; NumberOfPoints  4 ; }
                   { GeoElement Quadrangle  ; NumberOfPoints  4 ; }
                   { GeoElement Tetrahedron ; NumberOfPoints  4 ; }
                   { GeoElement Hexahedron  ; NumberOfPoints  6 ; }
                   { GeoElement Prism       ; NumberOfPoints  9 ; } }
           }
         }
  }
}
