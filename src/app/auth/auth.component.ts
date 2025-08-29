import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-auth',
  templateUrl: './auth.component.html',
  standalone: true,
  styleUrls: ['./auth.component.css']
})
export class AuthComponent implements OnInit {
  mode: 'login' | 'register' = 'login';

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.mode = params['mode'] === 'register' ? 'register' : 'login';
    });
  }
}
