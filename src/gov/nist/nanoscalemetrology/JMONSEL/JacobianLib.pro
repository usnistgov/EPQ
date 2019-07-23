/* -------------------------------------------------------------------
   File "Jacobian_Lib.pro"

   Definition of a jacobian method
   -------------------------------------------------------------------

   I N P U T
   ---------

   GlobalGroup :
   -----------
   DomainInf                Regions with Spherical Shell Transformation

   Parameters :
   ----------
   Val_Rint, Val_Rext       Inner and outer radius of the Spherical Shell
                            of DomainInf
*/

Group {
  DefineGroup[ DomainInf ] ;
  DefineVariable[ Val_Rint, Val_Rext ] ;
}

Jacobian {
  { Name Vol ;
    Case { { Region DomainInf ;
             Jacobian VolSphShell {Val_Rint, Val_Rext} ; }
           { Region All ; Jacobian Vol ; }
    }
  }
  { Name Sur ;
    Case { 
      { Region All ; Jacobian Sur ; }
    }
  }

}
