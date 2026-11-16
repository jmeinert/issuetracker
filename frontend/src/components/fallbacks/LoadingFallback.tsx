import { FullPageCenter } from '../ui/FullPageCenter';
import { Loader } from '@mantine/core';

export function LoadingFallback() {
  return (
    <FullPageCenter>
      <Loader color="blue" />
    </FullPageCenter>
  );
}
