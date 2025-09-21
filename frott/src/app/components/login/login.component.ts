import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService, LoginResponse, User } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

interface LoginPayload {
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  message: string = '';
  messageType: 'success' | 'error' | '' = '';
  isLoading: boolean = false;
  showPassword: boolean = false;

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      rememberMe: [false]
    });
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading = true;

      const payload: LoginPayload = {
        email: this.loginForm.value.email,
        password: this.loginForm.value.password
      };
      this.authService.login(payload).subscribe({
        next: (res: LoginResponse) => {
          this.isLoading = false;
          this.message = res.message || 'Connexion réussie !';
          this.messageType = 'success';

          localStorage.setItem('token', res.token);
          localStorage.setItem('roles', JSON.stringify(res.roles || []));

          const user: User = {
            username: res.username,
            email: res.email,
            roles: res.roles || []
          };
          localStorage.setItem('user', JSON.stringify(user));

          if (this.authService.isAdmin()) {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/']);
          }
        },
        error: (err) => {
          this.isLoading = false;
          this.message = 'Erreur : ' + (err.error?.message || 'Échec de la connexion');
          this.messageType = 'error';
          console.error('Login error:', err);
        }
      });
    }
  }
}