import { AfterViewInit, Directive, ElementRef } from '@angular/core';

@Directive({
  selector: '[formFieldError]',
  standalone: true,
})
export class FormErrorDirective implements AfterViewInit {
  constructor(public el: ElementRef<HTMLElement>) {}
  ngAfterViewInit(): void {
    // TODO
    //this.el.nativeElement.classList.add('ng-invalid', 'ng-dirty');
  }
}
