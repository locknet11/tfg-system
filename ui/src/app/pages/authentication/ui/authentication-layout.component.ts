import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-authentication-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './authentication-layout.component.html',
  styles: [
    '.auth__layout {height: 100vh; background: url("../../../../assets/img/background.svg") no-repeat center center fixed; -webkit-background-size: cover; -moz-background-size: cover; -o-background-size: cover; background-size: cover;}',
  ],
})
export class AuthenticationLayoutComponent {}
