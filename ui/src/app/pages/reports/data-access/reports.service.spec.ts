import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { Report, ReportHistoryResponse } from './reports.model';
import { ReportsService } from './reports.service';

describe('ReportsService', () => {
  let service: ReportsService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.baseUrl}/api/reports`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ReportsService],
    });
    service = TestBed.inject(ReportsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('posts filters to generate a report', () => {
    const stub = { id: 'r-1' } as Report;
    let result: Report | undefined;

    service.generate({ severities: ['CRITICAL'] }).subscribe(r => (result = r));

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.severities).toEqual(['CRITICAL']);
    req.flush(stub);

    expect(result?.id).toBe('r-1');
  });

  it('requests paged history newest first', () => {
    const stub: ReportHistoryResponse = { content: [], totalElements: 0 };

    service.list(0, 20).subscribe();

    const req = httpMock.expectOne(
      r => r.url === apiUrl && r.params.get('page') === '0'
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(stub);
  });

  it('fetches a single report by id', () => {
    const stub = { id: 'r-9' } as Report;

    service.get('r-9').subscribe();

    const req = httpMock.expectOne(`${apiUrl}/r-9`);
    expect(req.request.method).toBe('GET');
    req.flush(stub);
  });
});
